package com.lmi.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.request.UpdateCityRequest;
import com.lmi.crm.dto.request.UpdateUserRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.LicenseeCity;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.mapper.LicenseeMapper;
import com.lmi.crm.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseeCityRepository licenseeCityRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mlo-user-id}")
    private Integer mloUserId;

    @Override
    @Transactional
    public LicenseeResponse addLicensee(AddLicenseeRequest request, Integer requestingUserId) {
        boolean hasPrimary = request.getCities().stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsPrimary()));
        if (!hasPrimary)
            throw new IllegalArgumentException("At least one city must be marked as primary");

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new IllegalArgumentException("A user with this email already exists");

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = userMapper.fromAddLicenseeRequest(request, tempPassword);
        user = userRepository.save(user);
        final Integer userId = user.getId();

        List<LicenseeCity> cities = request.getCities().stream()
                .map(c -> LicenseeCity.builder()
                        .licenseeId(userId)
                        .city(c.getCity())
                        .isPrimary(Boolean.TRUE.equals(c.getIsPrimary()))
                        .build())
                .toList();

        List<LicenseeCity> savedCities = licenseeCityRepository.saveAll(cities);

        // TODO: replace with real invitation token when auth is built
        String inviteLink = baseUrl + "/register?token=PENDING";
        notificationService.sendInviteEmail(user.getEmail(), inviteLink, tempPassword);

        log.info("Licensee created — id: {}, email: {}, createdBy: {}", userId, user.getEmail(), requestingUserId);

        return LicenseeMapper.toResponse(user, savedCities);
    }

    @Override
    public String requestAssociateCreation(RequestAssociateCreationRequest request, Integer requestingLicenseeId) {
        User licensee = userRepository.findById(requestingLicenseeId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingLicenseeId));
        if (licensee.getRole() != UserRole.LICENSEE)
            throw new RuntimeException("Only a Licensee can request associate creation");

        String descriptionJson;
        try {
            descriptionJson = objectMapper.writeValueAsString(Map.of(
                    "firstName", request.getFirstName(),
                    "lastName", request.getLastName(),
                    "email", request.getEmail(),
                    "phone", request.getPhone()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize associate details", e);
        }

        alertService.createAlert(
                AlertType.ASSOCIATE_CREATION_REQUEST,
                "Associate Creation Request — " + request.getFirstName() + " " + request.getLastName(),
                descriptionJson,
                RelatedEntityType.USER,
                requestingLicenseeId,
                requestingLicenseeId,
                true
        );

        log.info("Associate creation request submitted by licenseeId: {} for: {} {}",
                requestingLicenseeId, request.getFirstName(), request.getLastName());

        return "Associate creation request submitted successfully";
    }

    @Override
    @Transactional
    public ApiResponse<UserResponse> approveRejectAssociateCreation(Integer alertId, boolean approve, Integer requestingAdminId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        if (alert.getStatus() != AlertStatus.PENDING)
            throw new RuntimeException("Alert has already been acted on");

        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingAdminId));
        if (admin.getRole() != UserRole.ADMIN && admin.getRole() != UserRole.SUPER_ADMIN)
            throw new RuntimeException("Only an Admin can approve or reject associate creation requests");

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("Associate creation request rejected — alertId: {}, adminId: {}", alertId, requestingAdminId);
            return ApiResponse.rejected("Associate creation request rejected");
        }

        Map<String, String> details;
        try {
            details = objectMapper.readValue(alert.getDescription(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse associate details from alert", e);
        }

        String email = details.get("email");
        if (userRepository.findByEmail(email).isPresent())
            throw new RuntimeException("A user with this email already exists");

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User associate = userMapper.forAssociate(
                details.get("firstName"),
                details.get("lastName"),
                email,
                details.get("phone"),
                tempPassword,
                alert.getRelatedEntityId()
        );
        associate = userRepository.save(associate);

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        // TODO: replace with real invitation token when auth is built
        String inviteLink = baseUrl + "/register?token=PENDING";
        notificationService.sendInviteEmail(associate.getEmail(), inviteLink, tempPassword);

        log.info("Associate created — id: {}, email: {}, licenseeId: {}, approvedBy: {}",
                associate.getId(), associate.getEmail(), associate.getLicenseeId(), requestingAdminId);

        return ApiResponse.success("Associate created successfully", userMapper.toResponse(associate));
    }

    @Override
    public List<UserResponse> getUsers(Integer requestingUserId, UserRole roleFilter, UserStatus statusFilter, boolean includeAllStatuses) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingUserId));

        UserStatus effectiveStatus;
        if (requestingUser.getRole() == UserRole.LICENSEE) {
            effectiveStatus = statusFilter != null ? statusFilter : UserStatus.ACTIVE;
        } else {
            effectiveStatus = includeAllStatuses ? null : (statusFilter != null ? statusFilter : UserStatus.ACTIVE);
        }

        List<User> users;
        switch (requestingUser.getRole()) {
            case LICENSEE:
                users = userRepository.findAssociatesByLicensee(requestingUserId, UserRole.ASSOCIATE, effectiveStatus);
                break;
            case ADMIN:
            case SUPER_ADMIN:
                users = userRepository.findByOptionalFilters(roleFilter, effectiveStatus);
                break;
            default:
                throw new RuntimeException("Access denied");
        }

        return users.stream().map(userMapper::toResponse).toList();
    }

    @Override
    public UserResponse getUserDetail(Integer requestingUserId, Integer targetUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingUserId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetUserId));

        boolean isSelf = requestingUserId.equals(targetUserId);

        if (!isSelf) {
            switch (requestingUser.getRole()) {
                case LICENSEE:
                    if (!requestingUserId.equals(targetUser.getLicenseeId()))
                        throw new RuntimeException("Access denied");
                    break;
                case ADMIN:
                case SUPER_ADMIN:
                    break;
                default:
                    throw new RuntimeException("Access denied");
            }
        }

        UserResponse response = userMapper.toResponse(targetUser);

        if (targetUser.getRole() == UserRole.LICENSEE) {
            List<LicenseeCity> cities = licenseeCityRepository.findByLicenseeId(targetUser.getId());
            response.setCities(cities.stream().map(c -> {
                LicenseeResponse.CityInfo info = new LicenseeResponse.CityInfo();
                info.setId(c.getId());
                info.setCity(c.getCity());
                info.setIsPrimary(c.getIsPrimary());
                return info;
            }).toList());
        }

        return response;
    }

    @Override
    @Transactional
    public UserResponse updateUser(Integer requestingUserId, Integer targetUserId, UpdateUserRequest request) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingUserId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetUserId));

        boolean isSelf = requestingUserId.equals(targetUserId);
        UserRole requesterRole = requestingUser.getRole();
        boolean isAdmin = requesterRole == UserRole.ADMIN || requesterRole == UserRole.SUPER_ADMIN;

        if (!isSelf && !isAdmin)
            throw new RuntimeException("Access denied");

        UserRole originalRole = targetUser.getRole();

        // Basic fields — any caller can update on an allowed target
        if (request.getFirstName() != null) targetUser.setFirstName(request.getFirstName());
        if (request.getLastName() != null) targetUser.setLastName(request.getLastName());
        if (request.getPhone() != null) targetUser.setPhone(request.getPhone());

        // Email change — check for uniqueness
        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(targetUserId))
                    throw new RuntimeException("Email already in use");
            });
            targetUser.setEmail(request.getEmail());
        }

        // Status — ADMIN or SUPER_ADMIN only
        if (isAdmin && request.getStatus() != null)
            targetUser.setStatus(request.getStatus());

        // Role change — ADMIN or SUPER_ADMIN only
        if (isAdmin && request.getRole() != null) {
            if (requesterRole == UserRole.ADMIN) {
                if (originalRole != UserRole.ASSOCIATE)
                    throw new RuntimeException("Admin can only change role of Associates");
            } else if (requesterRole == UserRole.SUPER_ADMIN) {
                if (originalRole == UserRole.LICENSEE)
                    throw new RuntimeException("Licensee role cannot be changed");
            }

            targetUser.setRole(request.getRole());

            if (request.getRole() != UserRole.ASSOCIATE) {
                targetUser.setLicenseeId(null);
            } else {
                if (request.getNewLicenseeId() != null) {
                    targetUser.setLicenseeId(request.getNewLicenseeId());
                } else if (targetUser.getLicenseeId() == null) {
                    throw new RuntimeException("A licenseeId must be provided when setting role to ASSOCIATE");
                }
            }
        }

        // Associate reassignment without role change — ADMIN or SUPER_ADMIN only
        if (request.getRole() == null && request.getNewLicenseeId() != null && isAdmin && originalRole == UserRole.ASSOCIATE)
            targetUser.setLicenseeId(request.getNewLicenseeId());

        User savedUser = userRepository.save(targetUser);

        // City operations — only relevant when original role is LICENSEE
        if (originalRole == UserRole.LICENSEE && request.getCities() != null) {
            List<LicenseeCity> existingCities = licenseeCityRepository.findByLicenseeId(targetUserId);

            for (UpdateCityRequest cityReq : request.getCities()) {
                boolean alreadyExists = existingCities.stream()
                        .anyMatch(c -> c.getCity().equalsIgnoreCase(cityReq.getCity()));

                if (!cityReq.isDelete()) {
                    if (alreadyExists)
                        throw new RuntimeException("City already exists: " + cityReq.getCity());
                    licenseeCityRepository.save(LicenseeCity.builder()
                            .licenseeId(targetUserId)
                            .city(cityReq.getCity())
                            .isPrimary(false)
                            .build());
                } else {
                    LicenseeCity toDelete = existingCities.stream()
                            .filter(c -> c.getCity().equalsIgnoreCase(cityReq.getCity()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("City not found: " + cityReq.getCity()));
                    if (Boolean.TRUE.equals(toDelete.getIsPrimary()))
                        throw new RuntimeException("Cannot delete primary city");
                    licenseeCityRepository.delete(toDelete);
                }
            }
        }

        // Primary city change — ADMIN or SUPER_ADMIN only
        if (originalRole == UserRole.LICENSEE && isAdmin && request.getNewPrimaryCity() != null) {
            List<LicenseeCity> allCities = licenseeCityRepository.findByLicenseeId(targetUserId);
            LicenseeCity currentPrimary = allCities.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getIsPrimary()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No primary city found for licensee: " + targetUserId));

            allCities.stream()
                    .filter(c -> c.getCity().equalsIgnoreCase(request.getNewPrimaryCity()))
                    .findFirst()
                    .ifPresentOrElse(match -> {
                        if (Boolean.TRUE.equals(match.getIsPrimary())) {
                            // already primary — nothing to do
                        } else {
                            currentPrimary.setIsPrimary(false);
                            match.setIsPrimary(true);
                            licenseeCityRepository.saveAll(List.of(currentPrimary, match));
                        }
                    }, () -> {
                        currentPrimary.setIsPrimary(false);
                        licenseeCityRepository.save(currentPrimary);
                        licenseeCityRepository.save(LicenseeCity.builder()
                                .licenseeId(targetUserId)
                                .city(request.getNewPrimaryCity())
                                .isPrimary(true)
                                .build());
                    });
        }

        log.info("User updated — targetId: {}, requestedBy: {}", targetUserId, requestingUserId);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(Integer requestingUserId, Integer targetUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingUserId));

        UserRole requesterRole = requestingUser.getRole();
        if (requesterRole != UserRole.ADMIN && requesterRole != UserRole.SUPER_ADMIN)
            throw new RuntimeException("Access denied");

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetUserId));

        if (targetUser.getStatus() == UserStatus.INACTIVE)
            throw new RuntimeException("User is already inactive");

        UserRole targetRole = targetUser.getRole();

        if (requesterRole == UserRole.ADMIN && (targetRole == UserRole.ADMIN || targetRole == UserRole.SUPER_ADMIN))
            throw new RuntimeException("Admin cannot deactivate another Admin");

        switch (targetRole) {
            case ASSOCIATE -> {
                // TODO: transfer all prospects where associateId = targetUserId to associate's parent licensee — implement after ProspectService is built
                targetUser.setStatus(UserStatus.INACTIVE);
            }
            case LICENSEE -> {
                // TODO: transfer all prospect_licensees where licenseeId = targetUserId to MLO — implement after ProspectService is built
                // TODO: reassign all associates under this licensee to MLO — implement after ProspectService is built
                targetUser.setStatus(UserStatus.INACTIVE);
            }
            default -> targetUser.setStatus(UserStatus.INACTIVE);
        }

        userRepository.save(targetUser);

        log.info("User deactivated — targetId: {}, requestedBy: {}", targetUserId, requestingUserId);

        String fullName = targetUser.getFirstName() + " " + targetUser.getLastName();
        String role = targetUser.getRole().name();
        List<User> admins = new ArrayList<>();
        admins.addAll(userRepository.findByRole(UserRole.ADMIN));
        admins.addAll(userRepository.findByRole(UserRole.SUPER_ADMIN));
        admins.forEach(admin -> notificationService.sendUserDeactivatedEmail(admin.getEmail(), fullName, role));

        return userMapper.toResponse(targetUser);
    }

    @Override
    public String requestAssociateDeactivation(Integer requestingLicenseeId, Integer targetAssociateId) {
        User requestingUser = userRepository.findById(requestingLicenseeId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingLicenseeId));
        if (requestingUser.getRole() != UserRole.LICENSEE)
            throw new RuntimeException("Access denied");

        User targetUser = userRepository.findById(targetAssociateId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + targetAssociateId));
        if (targetUser.getRole() != UserRole.ASSOCIATE)
            throw new RuntimeException("Target user is not an Associate");
        if (!requestingLicenseeId.equals(targetUser.getLicenseeId()))
            throw new RuntimeException("This Associate does not belong to your licensee");
        if (targetUser.getStatus() == UserStatus.INACTIVE)
            throw new RuntimeException("User is already inactive");

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.ASSOCIATE_DEACTIVATION_REQUEST, targetAssociateId, AlertStatus.PENDING
        ).ifPresent(a -> { throw new RuntimeException("A deactivation request for this Associate is already pending"); });

        alertService.createAlert(
                AlertType.ASSOCIATE_DEACTIVATION_REQUEST,
                "Associate Deactivation Request — " + targetUser.getFirstName() + " " + targetUser.getLastName(),
                "Licensee id " + requestingLicenseeId + " has requested deactivation of Associate id " + targetAssociateId,
                RelatedEntityType.USER,
                targetAssociateId,
                requestingLicenseeId,
                true
        );

        log.info("Associate deactivation requested — associateId: {}, requestedBy licenseeId: {}", targetAssociateId, requestingLicenseeId);

        return "Associate deactivation request submitted successfully";
    }

    @Override
    @Transactional
    public ApiResponse<UserResponse> approveRejectAssociateDeactivation(Integer requestingUserId, Integer alertId, boolean approve) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingUserId));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN)
            throw new RuntimeException("Access denied");

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        if (alert.getAlertType() != AlertType.ASSOCIATE_DEACTIVATION_REQUEST)
            throw new RuntimeException("Alert is not of type ASSOCIATE_DEACTIVATION_REQUEST");
        if (alert.getStatus() != AlertStatus.PENDING)
            throw new RuntimeException("Alert is no longer pending");

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("Associate deactivation rejected — alertId: {}, rejectedBy: {}", alertId, requestingUserId);
            return ApiResponse.rejected("Associate deactivation request rejected");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        UserResponse response = deactivateUser(requestingUserId, alert.getRelatedEntityId());

        log.info("Associate deactivation approved — associateId: {}, approvedBy: {}", alert.getRelatedEntityId(), requestingUserId);

        return ApiResponse.success("Associate deactivated successfully", response);
    }
}
