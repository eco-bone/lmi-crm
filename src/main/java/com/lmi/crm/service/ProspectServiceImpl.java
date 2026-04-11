package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.request.UpdateProspectRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProtectionStatus;
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

    @Override
    @Transactional
    public ProspectResponse updateProspect(Integer requestingUserId, Integer prospectId, UpdateProspectRequest request) {

        // Step 1 — Validate requesting user and prospect
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Prospect not found"));

        log.info("PUT /api/prospects/{} — requestingUserId: {}, role: {}", prospectId, requestingUserId, requestingUser.getRole());

        // Step 2 — Role and ownership check
        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            throw new RuntimeException("Access denied: Associates cannot update prospects");
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        // Step 3 — Apply standard fields
        prospectMapper.updateFromRequest(request, prospect);

        // Step 4 — Apply admin-only fields
        if (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN) {
            if (request.getProtectionStatus() != null) prospect.setProtectionStatus(request.getProtectionStatus());
            if (request.getProtectionPeriodMonths() != null) prospect.setProtectionPeriodMonths(request.getProtectionPeriodMonths());
        }

        // Step 5 — Licensee reassignment (Admin/Super Admin only)
        if (request.getNewLicenseeId() != null
                && (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN)) {
            User newLicensee = userRepository.findById(request.getNewLicenseeId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (newLicensee.getRole() != UserRole.LICENSEE) {
                throw new RuntimeException("Target user is not a licensee");
            }
            ProspectLicensee existing = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospectId)
                    .orElse(null);
            if (existing != null) {
                existing.setLicenseeId(request.getNewLicenseeId());
                prospectLicenseeRepository.save(existing);
            } else {
                prospectLicenseeRepository.save(prospectMapper.toProspectLicensee(prospectId, request.getNewLicenseeId()));
            }
        }

        // Step 6 — Save and return
        Prospect savedProspect = prospectRepository.save(prospect);
        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospectId)
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);
        log.info("Prospect updated — id: {}, requestedBy: {}", prospectId, requestingUserId);
        return prospectMapper.toResponse(savedProspect, licenseeId, null);
    }

    @Override
    @Transactional
    public String softDeleteProspect(Integer requestingUserId, Integer prospectId) {

        // Step 1 — Validate
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        log.info("DELETE /api/prospects/{} — requestingUserId: {}", prospectId, requestingUserId);

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Prospect not found"));

        // Step 2 — Soft delete
        prospect.setDeletionStatus(true);
        prospect.setProtectionStatus(ProtectionStatus.UNPROTECTED);
        prospectRepository.save(prospect);
        log.info("Prospect soft deleted — id: {}, deletedBy: {}", prospectId, requestingUserId);
        return "Prospect deleted successfully";
    }

    @Override
    public ProspectResponse getProspectDetail(Integer requestingUserId, Integer prospectId) {

        // Step 1 — Validate requesting user and prospect
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Prospect not found"));

        log.info("GET /api/prospects/{} — requestingUserId: {}, role: {}", prospectId, requestingUserId, requestingUser.getRole());

        // Step 2 — Ownership check for non-admin roles
        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            if (!requestingUserId.equals(prospect.getAssociateId())) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
        } else if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        // Step 3 — Return response based on role
        ProspectResponse response;
        if (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN) {
            Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospectId)
                    .map(ProspectLicensee::getLicenseeId)
                    .orElse(null);
            response = prospectMapper.toResponse(prospect, licenseeId, null);
        } else {
            response = prospectMapper.toLimitedResponse(prospect);
        }

        log.info("GET /api/prospects/{} — returned for userId: {}", prospectId, requestingUserId);

        return response;
    }

    @Override
    public String requestConversion(Integer requestingUserId, Integer prospectId) {

        // Step 1 — Validate requesting user and prospect
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.ASSOCIATE && requestingUser.getRole() != UserRole.LICENSEE) {
            throw new RuntimeException("Access denied");
        }

        log.info("POST /api/prospects/{}/convert — requestingUserId: {}", prospectId, requestingUserId);

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Prospect not found"));

        // Step 2 — Validate prospect state
        if (prospect.getType() == ProspectType.CLIENT) {
            throw new RuntimeException("Prospect is already a client");
        }

        // Step 3 — Ownership check
        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            if (!requestingUserId.equals(prospect.getAssociateId())) {
                throw new RuntimeException("Access denied");
            }
        } else {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
        }

        // Step 4 — Check no existing pending conversion request
        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.PROSPECT_CONVERSION_REQUEST, prospectId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    throw new RuntimeException("A conversion request for this prospect is already pending");
                });

        // Step 5 — Create alert
        alertService.createAlert(
                AlertType.PROSPECT_CONVERSION_REQUEST,
                "Conversion Request — " + prospect.getCompanyName(),
                "User id " + requestingUserId + " has requested conversion of prospect: "
                        + prospect.getCompanyName() + " (id: " + prospectId + ") to Client",
                RelatedEntityType.PROSPECT,
                prospectId,
                requestingUserId,
                true
        );

        // Step 6 — Log and return
        log.info("Conversion requested — prospectId: {}, requestedBy: {}", prospectId, requestingUserId);
        return "Conversion request submitted successfully. Awaiting admin approval.";
    }

    @Override
    @Transactional
    public ApiResponse<ProspectResponse> approveRejectConversion(Integer requestingUserId, Integer alertId, boolean approve) {

        // Step 1 — Validate requesting user
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        log.info("PUT /api/prospects/conversions/{} — requestingUserId: {}, approve: {}", alertId, requestingUserId, approve);

        // Step 2 — Validate alert
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (alert.getAlertType() != AlertType.PROSPECT_CONVERSION_REQUEST) {
            throw new RuntimeException("Alert is not a conversion request");
        }
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new RuntimeException("Alert is no longer pending");
        }

        // Step 3 — Reject path
        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("Conversion rejected — alertId: {}, rejectedBy: {}", alertId, requestingUserId);
            return ApiResponse.rejected("Conversion request rejected");
        }

        // Step 4 — Approve path
        Prospect prospect = prospectRepository.findById(alert.getRelatedEntityId())
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new RuntimeException("Prospect not found"));

        prospect.setType(ProspectType.CLIENT);
        prospectRepository.save(prospect);

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);

        log.info("Conversion approved — prospectId: {}, approvedBy: {}", prospect.getId(), requestingUserId);
        return ApiResponse.success("Prospect converted to client successfully", prospectMapper.toResponse(prospect, licenseeId, null));
    }
}
