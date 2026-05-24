package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.request.UpdateProspectRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.DuplicateCheckResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.dto.response.ProspectsPageResponse;
import com.lmi.crm.dto.response.ProspectsSummaryResponse;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProvisionalDecision;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.ProspectMapper;
import com.lmi.crm.util.FuzzyMatchUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Override
    @Transactional
    public ProspectResponse addProspect(AddProspectRequest request, Integer requestingUserId) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE && requestingUser.getRole() != UserRole.ASSOCIATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Integer effectiveLicenseeId;
        Integer associateId;

        if (requestingUser.getRole() == UserRole.LICENSEE) {
            effectiveLicenseeId = requestingUserId;
            associateId = request.getAssociateId();
        } else {
            if (requestingUser.getLicenseeId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Associate is not linked to a licensee");
            }
            effectiveLicenseeId = requestingUser.getLicenseeId();
            associateId = requestingUserId;
        }

        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhone() != null && !request.getPhone().isBlank();
        if (!hasEmail && !hasPhone) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one of email or phone number is required");
        }

        if (hasEmail) {
            prospectRepository.findByEmailIgnoreCaseAndDeletionStatusFalse(request.getEmail())
                    .ifPresent(p -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A prospect with email '" + request.getEmail() + "' already exists in the system");
                    });
        }

        prospectRepository.findByContactFirstNameIgnoreCaseAndContactLastNameIgnoreCaseAndCompanyNameIgnoreCaseAndDeletionStatusFalse(
                request.getContactFirstName(), request.getContactLastName(), request.getCompanyName())
                .ifPresent(p -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                        "Duplicate Prospect Detected: %s %s at %s already exists in the system",
                        p.getContactFirstName(), p.getContactLastName(), p.getCompanyName()
                    ));
                });

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

        Prospect prospect = prospectMapper.fromAddProspectRequest(request, associateId, requestingUserId, isProvisional);
        Prospect savedProspect = prospectRepository.save(prospect);

        prospectLicenseeRepository.save(prospectMapper.toProspectLicensee(savedProspect.getId(), effectiveLicenseeId));

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

        log.info("Prospect created — id: {}, company: {}, licenseeId: {}, associateId: {}, provisional: {}",
                savedProspect.getId(), savedProspect.getCompanyName(),
                effectiveLicenseeId, associateId, isProvisional);

        Map<String, Object> auditMeta = isProvisional
                ? Map.of("provisional", true, "reasons", combinedDescription, "licenseeId", effectiveLicenseeId)
                : Map.of("licenseeId", effectiveLicenseeId);
        auditService.log(AuditActionType.PROSPECT_CREATED, RelatedEntityType.PROSPECT, savedProspect.getId(),
                requestingUserId, null, auditService.snapshot(savedProspect), auditMeta);

        return prospectMapper.toResponse(savedProspect, effectiveLicenseeId, isProvisional ? combinedDescription : null);
    }

    @Override
    public String requestProtectionExtension(Integer prospectId, Integer requestingUserId) {

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE && requestingUser.getRole() != UserRole.ASSOCIATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        boolean isOwner;
        if (requestingUser.getRole() == UserRole.LICENSEE) {
            isOwner = prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId);
        } else {
            isOwner = prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUser.getLicenseeId());
        }

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have ownership of this prospect");
        }

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.PROTECTION_EXTENSION_REQUEST, prospectId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A protection extension request is already pending for this prospect");
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

        prospect.setExtensionRequestPending(true);
        prospectRepository.save(prospect);

        log.info("Protection extension requested — prospectId: {}, requestedBy: {}", prospectId, requestingUserId);

        return "Protection extension request submitted successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public Object getProspects(Integer requestingUserId, boolean getAll, ProspectType typeFilter,
                               Integer licenseeIdFilter, Integer associateIdFilter, int page, int limit) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        log.info("getProspects — requestingUserId: {}, role: {}, getAll: {}, typeFilter: {}, licenseeIdFilter: {}, associateIdFilter: {}, page: {}, limit: {}",
                requestingUserId, requestingUser.getRole(), getAll, typeFilter, licenseeIdFilter, associateIdFilter, page, limit);

        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;

        List<Prospect> prospects;
        if (getAll) {
            if (typeFilter != null) {
                prospects = prospectRepository.findByDeletionStatusFalseAndType(typeFilter);
            } else {
                prospects = prospectRepository.findByDeletionStatusFalse();
            }

        } else if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            prospects = prospectRepository.findByAssociateIdAndDeletionStatusFalse(requestingUserId);
            if (typeFilter != null) {
                prospects = prospects.stream().filter(p -> p.getType() == typeFilter).toList();
            }

        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            List<Integer> prospectIds = prospectLicenseeRepository.findByLicenseeId(requestingUserId)
                    .stream().map(ProspectLicensee::getProspectId).toList();
            prospects = prospectRepository.findByIdInAndDeletionStatusFalse(prospectIds);
            if (typeFilter != null) {
                prospects = prospects.stream().filter(p -> p.getType() == typeFilter).toList();
            }

        } else if (isAdmin) {
            if (licenseeIdFilter != null) {
                List<Integer> prospectIds = prospectLicenseeRepository.findByLicenseeId(licenseeIdFilter)
                        .stream().map(ProspectLicensee::getProspectId).toList();
                prospects = prospectRepository.findByIdInAndDeletionStatusFalse(prospectIds);
                if (associateIdFilter != null) {
                    prospects = prospects.stream().filter(p -> associateIdFilter.equals(p.getAssociateId())).toList();
                }
                if (typeFilter != null) {
                    prospects = prospects.stream().filter(p -> p.getType() == typeFilter).toList();
                }
            } else if (associateIdFilter != null) {
                prospects = prospectRepository.findByAssociateIdAndDeletionStatusFalse(associateIdFilter);
                if (typeFilter != null) {
                    prospects = prospects.stream().filter(p -> p.getType() == typeFilter).toList();
                }
            } else if (typeFilter != null) {
                prospects = prospectRepository.findByDeletionStatusFalseAndType(typeFilter);
            } else {
                prospects = prospectRepository.findByDeletionStatusFalse();
            }

        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        long overallTotal = prospects.size();
        long prospectCount = prospects.stream().filter(p -> p.getType() == ProspectType.PROSPECT).count();
        long clientCount = prospects.stream().filter(p -> p.getType() == ProspectType.CLIENT).count();
        long provisionalCount = prospects.stream().filter(p -> p.getStatus() == ProspectStatus.PROVISIONAL).count();
        long unprotectedCount = prospects.stream().filter(p -> p.getStatus() == ProspectStatus.UNPROTECTED).count();

        Long globalTotal = null;
        Long globalPending = null;
        if (isAdmin) {
            globalTotal = prospectRepository.countByDeletionStatusFalse();
            globalPending = prospectRepository.countByStatusAndDeletionStatusFalse(ProspectStatus.PROVISIONAL);
        }

        boolean useFullResponse = true;
        List<Integer> allProspectIds = prospects.stream().map(Prospect::getId).toList();
        Map<Integer, Integer> licenseeMap = (useFullResponse && !allProspectIds.isEmpty())
                ? prospectLicenseeRepository.findByProspectIdInAndIsPrimaryTrue(allProspectIds)
                        .stream().collect(Collectors.toMap(ProspectLicensee::getProspectId, ProspectLicensee::getLicenseeId))
                : Map.of();

        List<ProspectResponse> allResponses = prospects.stream()
                .map(p -> useFullResponse
                        ? prospectMapper.toResponse(p, licenseeMap.get(p.getId()), null)
                        : prospectMapper.toLimitedResponse(p))
                .toList();

        log.info("getProspects — requestingUserId: {}, overallTotal: {}, prospectCount: {}, clientCount: {}",
                requestingUserId, overallTotal, prospectCount, clientCount);

        int start = page * limit;
        int end = Math.min(start + limit, allResponses.size());
        List<ProspectResponse> pageContent = start < allResponses.size()
                ? allResponses.subList(start, end)
                : List.of();
        Page<ProspectResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), allResponses.size());

        log.info("getProspects — requestingUserId: {}, getAll: {}, page: {}, limit: {}", requestingUserId, getAll, page, limit);

        return ProspectsPageResponse.builder()
                .overallTotal(overallTotal)
                .prospectCount(prospectCount)
                .clientCount(clientCount)
                .provisionalCount(provisionalCount)
                .unprotectedCount(unprotectedCount)
                .globalTotal(globalTotal)
                .globalPending(globalPending)
                .prospects(pageResult)
                .build();
    }

    @Override
    @Transactional
    public ProspectResponse updateProspect(Integer requestingUserId, Integer prospectId, UpdateProspectRequest request) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        log.info("PUT /api/prospects/{} — requestingUserId: {}, role: {}", prospectId, requestingUserId, requestingUser.getRole());

        Map<String, Object> previousState = auditService.snapshot(prospect);

        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Associates cannot update prospects");
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        } else if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        prospectMapper.updateFromRequest(request, prospect);

        if (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN) {
            if (request.getStatus() != null) {
                prospect.setStatus(request.getStatus());
                if (request.getStatus() != ProspectStatus.PROVISIONAL) {
                    resolveAlertIfPresent(AlertType.DUPLICATE_PROSPECT, prospectId);
                }
            }
            if (request.getProtectionPeriodMonths() != null) {
                prospect.setProtectionPeriodMonths(request.getProtectionPeriodMonths());
                resolveAlertIfPresent(AlertType.PROTECTION_EXTENSION_REQUEST, prospectId);
                resolveAlertIfPresent(AlertType.PROSPECT_PROTECTION_WARNING, prospectId);
                resolveAlertIfPresent(AlertType.PROSPECT_UNPROTECTED, prospectId);
            }
        }

        if (request.getNewLicenseeId() != null
                && (requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN)) {
            User newLicensee = userRepository.findById(request.getNewLicenseeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (newLicensee.getRole() != UserRole.LICENSEE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a licensee");
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

        if (request.getNewAssociateId() != null) {
            boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;
            boolean isLicensee = requestingUser.getRole() == UserRole.LICENSEE;
            if (!isAdmin && !isLicensee) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            User newAssociate = userRepository.findById(request.getNewAssociateId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Associate not found with id: " + request.getNewAssociateId()));
            if (newAssociate.getRole() != UserRole.ASSOCIATE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not an associate");
            }
            if (isLicensee && !requestingUserId.equals(newAssociate.getLicenseeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Associate does not belong to your licensee");
            }
            prospect.setAssociateId(request.getNewAssociateId());
        }

        Prospect savedProspect = prospectRepository.save(prospect);
        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospectId)
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);
        log.info("Prospect updated — id: {}, requestedBy: {}", prospectId, requestingUserId);

        auditService.log(AuditActionType.PROSPECT_UPDATED, RelatedEntityType.PROSPECT, prospectId,
                requestingUserId, previousState, auditService.snapshot(savedProspect), null);

        String recordType = savedProspect.getType() == ProspectType.CLIENT ? "Client" : "Prospect";
        Set<String> emails = new HashSet<>();
        userRepository.findByRole(UserRole.ADMIN).forEach(u -> emails.add(u.getEmail()));
        userRepository.findByRole(UserRole.SUPER_ADMIN).forEach(u -> emails.add(u.getEmail()));
        if (licenseeId != null) {
            userRepository.findById(licenseeId).ifPresent(u -> emails.add(u.getEmail()));
        }
        if (savedProspect.getAssociateId() != null) {
            userRepository.findById(savedProspect.getAssociateId()).ifPresent(u -> emails.add(u.getEmail()));
        }
        emails.forEach(email -> notificationService.sendRecordUpdatedEmail(email, recordType, savedProspect.getCompanyName()));

        return prospectMapper.toResponse(savedProspect, licenseeId, null);
    }

    @Override
    @Transactional
    public String softDeleteProspect(Integer requestingUserId, Integer prospectId) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.info("DELETE /api/prospects/{} — requestingUserId: {}", prospectId, requestingUserId);

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        Map<String, Object> deletedState = auditService.snapshot(prospect);
        prospect.setDeletionStatus(true);
        prospect.setStatus(ProspectStatus.UNPROTECTED);
        prospectRepository.save(prospect);

        auditService.log(AuditActionType.PROSPECT_DELETED, RelatedEntityType.PROSPECT, prospectId,
                requestingUserId, deletedState, null, null);

        resolveAlertIfPresent(AlertType.PROSPECT_CONVERSION_REQUEST, prospectId);
        resolveAlertIfPresent(AlertType.DUPLICATE_PROSPECT, prospectId);
        resolveAlertIfPresent(AlertType.PROTECTION_EXTENSION_REQUEST, prospectId);
        resolveAlertIfPresent(AlertType.PROSPECT_PROTECTION_WARNING, prospectId);
        resolveAlertIfPresent(AlertType.PROSPECT_UNPROTECTED, prospectId);

        log.info("Prospect soft deleted — id: {}, deletedBy: {}", prospectId, requestingUserId);
        return "Prospect deleted successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public ProspectResponse getProspectDetail(Integer requestingUserId, Integer prospectId) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        log.info("GET /api/prospects/{} — requestingUserId: {}, role: {}", prospectId, requestingUserId, requestingUser.getRole());

        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospectId)
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);
        ProspectResponse response = prospectMapper.toResponse(prospect, licenseeId, null);

        log.info("GET /api/prospects/{} — returned full details for userId: {}", prospectId, requestingUserId);

        return response;
    }

    @Override
    public String requestConversion(Integer requestingUserId, Integer prospectId) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.ASSOCIATE && requestingUser.getRole() != UserRole.LICENSEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.info("POST /api/prospects/{}/convert — requestingUserId: {}", prospectId, requestingUserId);

        Prospect prospect = prospectRepository.findById(prospectId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        if (prospect.getType() == ProspectType.CLIENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prospect is already a client");
        }

        if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            if (!requestingUserId.equals(prospect.getAssociateId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        } else {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, requestingUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.PROSPECT_CONVERSION_REQUEST, prospectId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A conversion request for this prospect is already pending");
                });

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

        log.info("Conversion requested — prospectId: {}, requestedBy: {}", prospectId, requestingUserId);

        auditService.log(AuditActionType.PROSPECT_CONVERSION_REQUESTED, RelatedEntityType.PROSPECT, prospectId,
                requestingUserId, null, null, Map.of("company", prospect.getCompanyName()));

        return "Conversion request submitted successfully. Awaiting admin approval.";
    }

    @Override
    @Transactional
    public ApiResponse<ProspectResponse> approveRejectConversion(Integer requestingUserId, Integer alertId, boolean approve) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.info("PUT /api/prospects/conversions/{} — requestingUserId: {}, approve: {}", alertId, requestingUserId, approve);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (alert.getAlertType() != AlertType.PROSPECT_CONVERSION_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert is not a conversion request");
        }
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is no longer pending");
        }

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("Conversion rejected — alertId: {}, rejectedBy: {}", alertId, requestingUserId);
            auditService.log(AuditActionType.PROSPECT_CONVERSION_REJECTED, RelatedEntityType.PROSPECT,
                    alert.getRelatedEntityId(), requestingUserId, null, null, Map.of("alertId", alertId));
            return ApiResponse.rejected("Conversion request rejected");
        }

        Prospect prospect = prospectRepository.findById(alert.getRelatedEntityId())
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        prospect.setType(ProspectType.CLIENT);
        prospectRepository.save(prospect);

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);

        log.info("Conversion approved — prospectId: {}, approvedBy: {}", prospect.getId(), requestingUserId);

        auditService.log(AuditActionType.PROSPECT_CONVERSION_APPROVED, RelatedEntityType.PROSPECT,
                prospect.getId(), requestingUserId, null, auditService.snapshot(prospect), Map.of("alertId", alertId));

        return ApiResponse.success("Prospect converted to client successfully", prospectMapper.toResponse(prospect, licenseeId, null));
    }

    @Override
    @Transactional
    public ApiResponse<ProspectResponse> approveRejectProvisional(Integer requestingUserId, Integer alertId, ProvisionalDecision decision) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.info("PUT /api/prospects/provisional/{} — requestingUserId: {}, decision: {}", alertId, requestingUserId, decision);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (alert.getAlertType() != AlertType.DUPLICATE_PROSPECT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert is not a provisional prospect alert");
        }
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is no longer pending");
        }

        Prospect prospect = prospectRepository.findById(alert.getRelatedEntityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        if (prospect.getStatus() != ProspectStatus.PROVISIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prospect is not provisional");
        }

        if (decision == ProvisionalDecision.APPROVE) {
            prospect.setStatus(ProspectStatus.PROTECTED);
            prospectRepository.save(prospect);
            alert.setStatus(AlertStatus.RESOLVED);
            alertRepository.save(alert);
            log.info("Provisional prospect approved — prospectId: {}, approvedBy: {}", prospect.getId(), requestingUserId);
            auditService.log(AuditActionType.PROVISIONAL_APPROVED, RelatedEntityType.PROSPECT,
                    prospect.getId(), requestingUserId, null, auditService.snapshot(prospect), Map.of("alertId", alertId));
            Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                    .map(ProspectLicensee::getLicenseeId)
                    .orElse(null);
            return ApiResponse.success("Provisional prospect approved successfully", prospectMapper.toResponse(prospect, licenseeId, null));

        } else if (decision == ProvisionalDecision.REJECT) {
            prospect.setDeletionStatus(true);
            prospect.setStatus(ProspectStatus.UNPROTECTED);
            prospectRepository.save(prospect);
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("Provisional prospect rejected — prospectId: {}, rejectedBy: {}", prospect.getId(), requestingUserId);
            auditService.log(AuditActionType.PROVISIONAL_REJECTED, RelatedEntityType.PROSPECT,
                    prospect.getId(), requestingUserId, null, null, Map.of("alertId", alertId));
            return ApiResponse.rejected("Provisional prospect rejected and removed");

        } else {
            alert.setStatus(AlertStatus.RESOLVED);
            alertRepository.save(alert);
            log.info("Provisional prospect ignored — prospectId: {}, ignoredBy: {}", prospect.getId(), requestingUserId);
            Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                    .map(ProspectLicensee::getLicenseeId)
                    .orElse(null);
            return ApiResponse.success("Provisional prospect left as-is", prospectMapper.toResponse(prospect, licenseeId, null));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProspectsPageResponse searchProspects(Integer requestingUserId, String q, String scope, ProspectType type, int page, int limit) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;
        boolean scopeAll = "all".equalsIgnoreCase(scope) || isAdmin;
        String keyword = "%" + q.trim() + "%";

        List<Prospect> prospects;
        if (scopeAll) {
            prospects = prospectRepository.searchAll(keyword);
        } else if (requestingUser.getRole() == UserRole.ASSOCIATE) {
            prospects = prospectRepository.searchByAssociateId(keyword, requestingUserId);
        } else if (requestingUser.getRole() == UserRole.LICENSEE) {
            List<Integer> ids = prospectLicenseeRepository.findByLicenseeId(requestingUserId)
                    .stream().map(ProspectLicensee::getProspectId).toList();
            prospects = ids.isEmpty() ? List.of() : prospectRepository.searchByIds(keyword, ids);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (type != null) {
            prospects = prospects.stream().filter(p -> p.getType() == type).toList();
        }

        long overallTotal = prospects.size();
        long prospectCount = prospects.stream().filter(p -> p.getType() == ProspectType.PROSPECT).count();
        long clientCount = prospects.stream().filter(p -> p.getType() == ProspectType.CLIENT).count();
        long provisionalCount = prospects.stream().filter(p -> p.getStatus() == ProspectStatus.PROVISIONAL).count();
        long unprotectedCount = prospects.stream().filter(p -> p.getStatus() == ProspectStatus.UNPROTECTED).count();

        List<Integer> allIds = prospects.stream().map(Prospect::getId).toList();
        Map<Integer, Integer> licenseeMap = (!allIds.isEmpty())
                ? prospectLicenseeRepository.findByProspectIdInAndIsPrimaryTrue(allIds)
                        .stream().collect(Collectors.toMap(ProspectLicensee::getProspectId, ProspectLicensee::getLicenseeId))
                : Map.of();

        List<ProspectResponse> allResponses = prospects.stream()
                .map(p -> prospectMapper.toResponse(p, licenseeMap.get(p.getId()), null))
                .toList();

        int start = page * limit;
        int end = Math.min(start + limit, allResponses.size());
        List<ProspectResponse> pageContent = start < allResponses.size()
                ? allResponses.subList(start, end) : List.of();
        Page<ProspectResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), allResponses.size());

        log.info("searchProspects — requestingUserId: {}, scope: {}, type: {}, total: {}", requestingUserId, scope, type, overallTotal);

        return ProspectsPageResponse.builder()
                .overallTotal(overallTotal)
                .prospectCount(prospectCount)
                .clientCount(clientCount)
                .provisionalCount(provisionalCount)
                .unprotectedCount(unprotectedCount)
                .prospects(pageResult)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<ProspectResponse> approveRejectExtension(Integer requestingUserId, Integer alertId, boolean approve, Integer extensionMonths) {

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        log.info("PUT /api/prospects/extensions/{} — requestingUserId: {}, approve: {}, extensionMonths: {}", alertId, requestingUserId, approve, extensionMonths);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        if (alert.getAlertType() != AlertType.PROTECTION_EXTENSION_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert is not a protection extension request");
        }
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is no longer pending");
        }

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);

            prospectRepository.findById(alert.getRelatedEntityId())
                    .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                    .ifPresent(p -> {
                        p.setExtensionRequestPending(false);
                        prospectRepository.save(p);
                    });

            log.info("Protection extension rejected — alertId: {}, rejectedBy: {}", alertId, requestingUserId);
            auditService.log(AuditActionType.PROSPECT_UPDATED, RelatedEntityType.PROSPECT,
                    alert.getRelatedEntityId(), requestingUserId, null, null,
                    Map.of("alertId", alertId, "extensionDecision", "REJECTED"));
            return ApiResponse.rejected("Protection extension request rejected");
        }

        if (extensionMonths == null || extensionMonths <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "extensionMonths must be a positive number when approving");
        }

        Prospect prospect = prospectRepository.findById(alert.getRelatedEntityId())
                .filter(p -> !Boolean.TRUE.equals(p.getDeletionStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prospect not found"));

        int current = prospect.getProtectionPeriodMonths() != null ? prospect.getProtectionPeriodMonths() : 0;
        prospect.setProtectionPeriodMonths(current + extensionMonths);
        prospect.setExtensionRequestPending(false);
        prospect.setProtectionExtendedAt(LocalDateTime.now());
        prospectRepository.save(prospect);

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        Integer licenseeId = prospectLicenseeRepository.findByProspectIdAndIsPrimaryTrue(prospect.getId())
                .map(ProspectLicensee::getLicenseeId)
                .orElse(null);

        log.info("Protection extension approved — prospectId: {}, extensionMonths: {}, newTotal: {}, approvedBy: {}",
                prospect.getId(), extensionMonths, prospect.getProtectionPeriodMonths(), requestingUserId);

        auditService.log(AuditActionType.PROSPECT_UPDATED, RelatedEntityType.PROSPECT,
                prospect.getId(), requestingUserId, null, auditService.snapshot(prospect),
                Map.of("alertId", alertId, "extensionDecision", "APPROVED", "extensionMonths", extensionMonths));

        return ApiResponse.success("Protection extended by " + extensionMonths + " month(s) successfully",
                prospectMapper.toResponse(prospect, licenseeId, null));
    }

    @Override
    public List<DuplicateCheckResponse> checkDuplicateProspects(String companyName) {
        if (companyName == null || companyName.trim().length() < 2) {
            return List.of();
        }

        String prefix = companyName.trim().substring(0, Math.min(2, companyName.trim().length()));
        List<Prospect> activeProspects = prospectRepository.findByCompanyNameStartingWithIgnoreCaseAndDeletionStatusFalse(prefix);

        List<DuplicateCheckResponse> duplicates = new ArrayList<>();
        for (Prospect existing : activeProspects) {
            double similarity = FuzzyMatchUtil.similarity(existing.getCompanyName(), companyName.trim());
            if (similarity >= 0.65) {
                DuplicateCheckResponse response = new DuplicateCheckResponse();
                response.setId(existing.getId().longValue());
                response.setCompanyName(existing.getCompanyName());
                response.setCity(existing.getCity());
                response.setStatus(existing.getStatus());
                response.setSimilarity(similarity);
                duplicates.add(response);
            }
        }

        log.debug("Duplicate check for '{}' found {} matches", companyName, duplicates.size());
        return duplicates;
    }

    private void resolveAlertIfPresent(AlertType alertType, Integer relatedEntityId) {
        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(alertType, relatedEntityId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    a.setStatus(AlertStatus.RESOLVED);
                    alertRepository.save(a);
                    log.info("Alert auto-resolved — alertId: {}, type: {}, relatedEntityId: {}", a.getId(), alertType, relatedEntityId);
                });
    }
}
