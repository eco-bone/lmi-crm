package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.ProspectMapper;
import com.lmi.crm.util.FuzzyMatchUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProspectServiceImpl implements ProspectService {

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseeCityRepository licenseeCityRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ProspectMapper prospectMapper;

    @Override
    @Transactional
    public ProspectResponse addProspect(AddProspectRequest request, Integer requestingUserId) {

        // Step 1 — Validate requesting user
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE && requestingUser.getRole() != UserRole.ASSOCIATE) {
            throw new RuntimeException("Access denied");
        }

        Integer effectiveLicenseeId;
        Integer associateId;

        if (requestingUser.getRole() == UserRole.LICENSEE) {
            effectiveLicenseeId = requestingUserId;
            associateId = null;
        } else {
            if (requestingUser.getLicenseeId() == null) {
                throw new RuntimeException("Associate is not linked to a licensee");
            }
            effectiveLicenseeId = requestingUser.getLicenseeId();
            associateId = requestingUserId;
        }

        // Step 2 — Hard duplicate check
        prospectRepository.findByContactFirstNameIgnoreCaseAndContactLastNameIgnoreCaseAndCompanyNameIgnoreCaseAndDeletionStatusFalse(
                request.getContactFirstName(), request.getContactLastName(), request.getCompanyName())
                .ifPresent(p -> {
                    throw new RuntimeException("A prospect with this contact already exists at this company");
                });

        // Step 3 — Fuzzy match + city check
        boolean isProvisional = false;
        List<String> provisionalReasons = new ArrayList<>();

        String prefix = request.getCompanyName().length() >= 2
                ? request.getCompanyName().substring(0, 2)
                : request.getCompanyName();
        List<Prospect> activeProspects = prospectRepository.findByCompanyNameStartingWithIgnoreCaseAndDeletionStatusFalse(prefix);
        for (Prospect existing : activeProspects) {
            double score = FuzzyMatchUtil.similarity(existing.getCompanyName(), request.getCompanyName());
            log.debug("Fuzzy match score for '{}' vs '{}': {}", request.getCompanyName(), existing.getCompanyName(), score);
            if (score >= 0.65) {
                isProvisional = true;
                provisionalReasons.add("Company name similar to existing prospect: " + existing.getCompanyName());
                break;
            }
        }

        if (request.getProgramType() != ProspectProgramType.SHC) {
            boolean cityMatched = licenseeCityRepository.findByLicenseeId(effectiveLicenseeId)
                    .stream()
                    .anyMatch(lc -> lc.getCity().equalsIgnoreCase(request.getCity()));
            if (!cityMatched) {
                isProvisional = true;
                provisionalReasons.add("Prospect city '" + request.getCity() + "' is outside licensee's registered cities");
            }
        }

        String combinedDescription = String.join(". ", provisionalReasons);

        if (isProvisional) {
            log.warn("Prospect flagged as provisional — company: {}, reasons: {}", request.getCompanyName(), combinedDescription);
        }

        // Step 4 — Build and save Prospect entity
        Prospect prospect = prospectMapper.fromAddProspectRequest(request, associateId, requestingUserId, isProvisional);
        Prospect savedProspect = prospectRepository.save(prospect);

        // Step 5 — Save ProspectLicensee
        prospectLicenseeRepository.save(prospectMapper.toProspectLicensee(savedProspect.getId(), effectiveLicenseeId));

        // Step 6 — Create Alert if provisional
        if (isProvisional) {
            alertService.createAlert(
                    AlertType.DUPLICATE_PROSPECT,
                    "Provisional Prospect — " + request.getCompanyName(),
                    combinedDescription,
                    RelatedEntityType.PROSPECT,
                    savedProspect.getId(),
                    requestingUserId,
                    true
            );
        }

        // Step 7 — Log and return
        log.info("Prospect created — id: {}, company: {}, licenseeId: {}, associateId: {}, provisional: {}",
                savedProspect.getId(), savedProspect.getCompanyName(),
                effectiveLicenseeId, associateId, isProvisional);

        return prospectMapper.toResponse(savedProspect, effectiveLicenseeId, isProvisional ? combinedDescription : null);
    }

    @Override
    public String requestProtectionExtension(Integer prospectId, Integer requestingUserId) {

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Prospect not found"));

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE && requestingUser.getRole() != UserRole.ASSOCIATE) {
            throw new RuntimeException("Access denied");
        }

        boolean isOwner;
        if (requestingUser.getRole() == UserRole.LICENSEE) {
            isOwner = prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId);
        } else {
            isOwner = prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUser.getLicenseeId());
        }

        if (!isOwner) {
            throw new RuntimeException("You do not have ownership of this prospect");
        }

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.PROTECTION_EXTENSION_REQUEST, prospectId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    throw new RuntimeException("A protection extension request is already pending for this prospect");
                });

        alertService.createAlert(
                AlertType.PROTECTION_EXTENSION_REQUEST,
                "Protection Extension Request — " + prospect.getCompanyName(),
                "User id " + requestingUserId + " has requested a protection period extension for prospect: "
                        + prospect.getCompanyName() + " (id: " + prospectId + ")",
                RelatedEntityType.PROSPECT,
                prospectId,
                requestingUserId,
                true
        );

        log.info("Protection extension requested — prospectId: {}, requestedBy: {}", prospectId, requestingUserId);

        return "Protection extension request submitted successfully";
    }

    @Override
    public List<ProspectResponse> getProspects(Integer requestingUserId, ProspectType typeFilter,
                                               Integer licenseeIdFilter, Integer associateIdFilter) {

        // Step 1 — Validate requesting user
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("GET /api/prospects — requestingUserId: {}, role: {}, typeFilter: {}, licenseeIdFilter: {}, associateIdFilter: {}",
                requestingUserId, requestingUser.getRole(), typeFilter, licenseeIdFilter, associateIdFilter);

        List<Prospect> prospects;

        // Step 2 — Role-scoped visibility
        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            prospects = prospectRepository.findByAssociateIdAndDeletionStatusFalse(requestingUserId);
            if (typeFilter != null) {
                prospects = prospects.stream()
                        .filter(p -> p.getType() == typeFilter)
                        .toList();
            }

        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            List<Integer> prospectIds = prospectLicenseeRepository.findByLicenseeId(requestingUserId)
                    .stream()
                    .map(ProspectLicensee::getProspectId)
                    .toList();
            prospects = prospectRepository.findByIdInAndDeletionStatusFalse(prospectIds);
            if (typeFilter != null) {
                prospects = prospects.stream()
                        .filter(p -> p.getType() == typeFilter)
                        .toList();
            }

        } else if (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN) {
            if (licenseeIdFilter != null) {
                List<Integer> prospectIds = prospectLicenseeRepository.findByLicenseeId(licenseeIdFilter)
                        .stream()
                        .map(ProspectLicensee::getProspectId)
                        .toList();
                prospects = prospectRepository.findByIdInAndDeletionStatusFalse(prospectIds);
                if (associateIdFilter != null) {
                    prospects = prospects.stream()
                            .filter(p -> associateIdFilter.equals(p.getAssociateId()))
                            .toList();
                }
                if (typeFilter != null) {
                    prospects = prospects.stream()
                            .filter(p -> p.getType() == typeFilter)
                            .toList();
                }
            } else if (associateIdFilter != null) {
                prospects = prospectRepository.findByAssociateIdAndDeletionStatusFalse(associateIdFilter);
                if (typeFilter != null) {
                    prospects = prospects.stream()
                            .filter(p -> p.getType() == typeFilter)
                            .toList();
                }
            } else if (typeFilter != null) {
                prospects = prospectRepository.findByDeletionStatusFalseAndType(typeFilter);
            } else {
                prospects = prospectRepository.findByDeletionStatusFalse();
            }

        } else {
            throw new RuntimeException("Access denied");
        }

        log.info("GET /api/prospects — returning {} prospects for userId: {}", prospects.size(), requestingUserId);

        // Step 4 — Field visibility by role
        if (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN) {
            List<Integer> prospectIds = prospects.stream().map(Prospect::getId).toList();
            Map<Integer, Integer> licenseeMap = prospectLicenseeRepository
                    .findByProspectIdInAndIsPrimaryTrue(prospectIds)
                    .stream()
                    .collect(Collectors.toMap(ProspectLicensee::getProspectId, ProspectLicensee::getLicenseeId));
            return prospects.stream()
                    .map(p -> prospectMapper.toResponse(p, licenseeMap.get(p.getId()), null))
                    .toList();
        } else {
            return prospects.stream()
                    .map(p -> prospectMapper.toLimitedResponse(p))
                    .toList();
        }
    }
}
