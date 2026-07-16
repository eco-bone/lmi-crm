package com.lmi.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddAssociateRequest;
import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
import com.lmi.crm.dto.request.ResetPasswordRequest;
import com.lmi.crm.dto.request.UpdateCityRequest;
import com.lmi.crm.dto.request.UpdateUserRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.ImportResult;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.dto.response.UserResponse;
import com.lmi.crm.dto.response.UsersSummaryResponse;
import com.lmi.crm.dto.response.UsersPageResponse;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.LicenseeCity;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.event.NotificationEvent;
import com.lmi.crm.mapper.LicenseeMapper;
import com.lmi.crm.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private ProspectRepository prospectRepository;

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mlo-user-id}")
    private Integer mloUserId;

    @Override
    @Transactional
    public LicenseeResponse addLicensee(AddLicenseeRequest request, Integer requestingUserId) {
        log.debug("addLicensee — requestingUserId: {}, email: {}, cities: {}", requestingUserId, request.getEmail(), request.getCities().size());

        boolean hasPrimary = request.getCities().stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsPrimary()));
        if (!hasPrimary) {
            log.warn("addLicensee — rejected — no primary city provided — requestingUserId: {}", requestingUserId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one city must be marked as primary");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("addLicensee — rejected — email already exists: {} — requestingUserId: {}", request.getEmail(), requestingUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            log.warn("addLicensee — rejected — phone already exists: {} — requestingUserId: {}", request.getPhone(), requestingUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this phone number already exists");
        }

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = userMapper.fromAddLicenseeRequest(request, tempPassword);
        String invitationToken = UUID.randomUUID().toString();
        user.setStatus(UserStatus.PENDING);
        user.setInvitationToken(invitationToken);
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

        String inviteLink = frontendUrl + "/setup-account?token=" + invitationToken;
        String userEmail = user.getEmail();
        eventPublisher.publishEvent(new NotificationEvent(this, "Invite email — userId: " + userId,
                ns -> ns.sendInviteEmail(userEmail, inviteLink, tempPassword)));

        log.info("Licensee created — id: {}, email: {}, createdBy: {}", userId, user.getEmail(), requestingUserId);

        auditService.log(AuditActionType.LICENSEE_CREATED, RelatedEntityType.USER, userId,
                requestingUserId, null, auditService.snapshot(user), null);

        return LicenseeMapper.toResponse(user, savedCities);
    }

    @Override
    @Transactional
    public UserResponse addAssociate(AddAssociateRequest request, Integer requestingAdminId) {
        log.debug("addAssociate — requestingAdminId: {}, email: {}, licenseeId: {}", requestingAdminId, request.getEmail(), request.getLicenseeId());

        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingAdminId));
        if (admin.getRole() != UserRole.ADMIN && admin.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("addAssociate — access denied — userId: {} role: {}", requestingAdminId, admin.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        userRepository.findById(request.getLicenseeId())
                .filter(u -> u.getRole() == UserRole.LICENSEE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Licensee not found with id: " + request.getLicenseeId()));

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("addAssociate — email already exists: {} — requestingAdminId: {}", request.getEmail(), requestingAdminId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            log.warn("addAssociate — phone already exists: {} — requestingAdminId: {}", request.getPhone(), requestingAdminId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this phone number already exists");
        }

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User associate = userMapper.forAssociate(
                request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPhone(),
                tempPassword, request.getLicenseeId()
        );
        String invitationToken = UUID.randomUUID().toString();
        associate.setStatus(UserStatus.PENDING);
        associate.setInvitationToken(invitationToken);
        associate = userRepository.save(associate);

        String inviteLink = frontendUrl + "/setup-account?token=" + invitationToken;
        String associateEmail = associate.getEmail();
        Integer associateId = associate.getId();
        eventPublisher.publishEvent(new NotificationEvent(this, "Invite email — associateId: " + associateId,
                ns -> ns.sendInviteEmail(associateEmail, inviteLink, tempPassword)));

        log.info("Associate created directly by admin — id: {}, email: {}, licenseeId: {}, createdBy: {}",
                associate.getId(), associate.getEmail(), associate.getLicenseeId(), requestingAdminId);

        auditService.log(AuditActionType.ASSOCIATE_CREATED, RelatedEntityType.USER, associate.getId(),
                requestingAdminId, null, auditService.snapshot(associate), null);

        return userMapper.toResponse(associate);
    }

    @Override
    @Transactional
    public UserResponse createAdmin(RequestAssociateCreationRequest request, Integer requestingSuperAdminId) {
        log.debug("createAdmin — requestingSuperAdminId: {}, email: {}", requestingSuperAdminId, request.getEmail());

        User requestingUser = userRepository.findById(requestingSuperAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingSuperAdminId));
        if (requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("createAdmin — access denied — requestingUserId: {} is not SUPER_ADMIN (role: {})", requestingSuperAdminId, requestingUser.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("createAdmin — rejected — email already exists: {} — requestingUserId: {}", request.getEmail(), requestingSuperAdminId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            log.warn("createAdmin — rejected — phone already exists: {} — requestingUserId: {}", request.getPhone(), requestingSuperAdminId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this phone number already exists");
        }

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = userMapper.forAdmin(request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPhone(), passwordEncoder.encode(tempPassword));
        String invitationToken = UUID.randomUUID().toString();
        user.setStatus(UserStatus.PENDING);
        user.setInvitationToken(invitationToken);
        User savedUser = userRepository.save(user);

        String inviteLink = frontendUrl + "/setup-account?token=" + invitationToken;
        eventPublisher.publishEvent(new NotificationEvent(this, "Invite email — userId: " + savedUser.getId(),
                ns -> ns.sendInviteEmail(savedUser.getEmail(), inviteLink, tempPassword)));

        log.info("Admin created — id: {}, email: {}, createdBy: {}", savedUser.getId(), savedUser.getEmail(), requestingSuperAdminId);

        auditService.log(AuditActionType.ADMIN_CREATED, RelatedEntityType.USER, savedUser.getId(),
                requestingSuperAdminId, null, auditService.snapshot(savedUser), null);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public String requestAssociateCreation(RequestAssociateCreationRequest request, Integer requestingLicenseeId) {
        log.debug("requestAssociateCreation — requestingLicenseeId: {}, associate: {} {}", requestingLicenseeId, request.getFirstName(), request.getLastName());

        User licensee = userRepository.findById(requestingLicenseeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingLicenseeId));
        if (licensee.getRole() != UserRole.LICENSEE) {
            log.warn("requestAssociateCreation — rejected — userId: {} is not LICENSEE (role: {})", requestingLicenseeId, licensee.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a Licensee can request associate creation");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("requestAssociateCreation — rejected — email already exists: {} — requestingLicenseeId: {}", request.getEmail(), requestingLicenseeId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            log.warn("requestAssociateCreation — rejected — phone already exists: {} — requestingLicenseeId: {}", request.getPhone(), requestingLicenseeId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this phone number already exists");
        }

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
        log.debug("approveRejectAssociateCreation — alertId: {}, approve: {}, requestingAdminId: {}", alertId, approve, requestingAdminId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found with id: " + alertId));
        if (alert.getStatus() != AlertStatus.PENDING) {
            log.warn("approveRejectAssociateCreation — alert already acted on — alertId: {}, status: {}", alertId, alert.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert has already been acted on");
        }

        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingAdminId));
        if (admin.getRole() != UserRole.ADMIN && admin.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("approveRejectAssociateCreation — access denied — userId: {} role: {}", requestingAdminId, admin.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Admin can approve or reject associate creation requests");
        }

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("approveRejectAssociateCreation — rejected — alertId: {}, adminId: {}", alertId, requestingAdminId);
            auditService.log(AuditActionType.ASSOCIATE_REQUEST_REJECTED, RelatedEntityType.USER,
                    alert.getRelatedEntityId(), requestingAdminId, null, null, Map.of("alertId", alertId));
            return ApiResponse.success("Associate creation request rejected", null);
        }

        log.debug("approveRejectAssociateCreation — approving — alertId: {}", alertId);

        Map<String, String> details;
        try {
            details = objectMapper.readValue(alert.getDescription(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse associate details from alert", e);
        }

        String email = details.get("email");
        if (userRepository.findByEmail(email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User associate = userMapper.forAssociate(
                details.get("firstName"),
                details.get("lastName"),
                email,
                details.get("phone"),
                tempPassword,
                alert.getRelatedEntityId()
        );
        String invitationToken = UUID.randomUUID().toString();
        associate.setStatus(UserStatus.PENDING);
        associate.setInvitationToken(invitationToken);
        associate = userRepository.save(associate);

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        String inviteLink = frontendUrl + "/setup-account?token=" + invitationToken;
        final User savedAssociate = associate;
        eventPublisher.publishEvent(new NotificationEvent(this, "Invite email — associateId: " + savedAssociate.getId(),
                ns -> ns.sendInviteEmail(savedAssociate.getEmail(), inviteLink, tempPassword)));

        userRepository.findById(savedAssociate.getLicenseeId()).ifPresent(licensee ->
                eventPublisher.publishEvent(new NotificationEvent(this,
                        "Associate approved email — associateId: " + savedAssociate.getId(),
                        ns -> ns.sendAssociateApprovedEmail(
                                licensee.getEmail(), savedAssociate.getFirstName(), savedAssociate.getLastName()))));

        log.info("Associate created — id: {}, email: {}, licenseeId: {}, approvedBy: {}",
                associate.getId(), associate.getEmail(), associate.getLicenseeId(), requestingAdminId);

        auditService.log(AuditActionType.ASSOCIATE_REQUEST_APPROVED, RelatedEntityType.USER, associate.getId(),
                requestingAdminId, null, auditService.snapshot(associate), Map.of("alertId", alertId));

        return ApiResponse.success("Associate created successfully", userMapper.toResponse(associate));
    }

    @Override
    public Object getUsers(Integer requestingUserId, boolean getAll, UserRole roleFilter, UserStatus statusFilter, boolean includeAllStatuses, Integer licenseeIdFilter, int page, int limit) {
        log.debug("getUsers — requestingUserId: {}, getAll: {}, roleFilter: {}, statusFilter: {}, includeAllStatuses: {}, licenseeIdFilter: {}, page: {}, limit: {}",
                requestingUserId, getAll, roleFilter, statusFilter, includeAllStatuses, licenseeIdFilter, page, limit);

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));

        boolean isLicensee = requestingUser.getRole() == UserRole.LICENSEE;
        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;

        // Full scoped list (no role/status filters) — used for summary counts
        List<User> scopedUsers;
        if (isLicensee || isAdmin) {
            scopedUsers = userRepository.findByOptionalFilters(null, null);
        } else {
            log.warn("getUsers — access denied — userId: {}, role: {}", requestingUserId, requestingUser.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Summary counts from full scoped list
        long overallTotal = scopedUsers.size();
        long activeCount = scopedUsers.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        long inactiveCount = scopedUsers.stream().filter(u -> u.getStatus() == UserStatus.INACTIVE).count();
        Map<UserRole, Long> countByRole = Arrays.stream(UserRole.values())
                .collect(Collectors.toMap(r -> r, r -> scopedUsers.stream().filter(u -> u.getRole() == r).count()));

        // Apply filters in-memory from scoped list
        UserStatus effectiveStatus;
        if (getAll) {
            effectiveStatus = null; // getAll bypasses status filter — return all statuses
        } else {
            effectiveStatus = includeAllStatuses ? null : (statusFilter != null ? statusFilter : UserStatus.ACTIVE);
        }

        if (licenseeIdFilter != null && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can filter by licensee");
        }

        // Licensees are always scoped to their own associates — they cannot see another licensee's roster,
        // and don't need to (or get to) pass licenseeId themselves to achieve this.
        Integer effectiveLicenseeId = isLicensee ? requestingUser.getId() : licenseeIdFilter;

        final UserRole rf = roleFilter;
        final UserStatus es = effectiveStatus;
        final Integer lf = effectiveLicenseeId;
        List<User> filteredUsers = scopedUsers.stream()
                .filter(u -> rf == null || u.getRole() == rf)
                .filter(u -> es == null || u.getStatus() == es)
                .filter(u -> lf == null || lf.equals(u.getLicenseeId()))
                .toList();

        List<UserResponse> filteredResponses = filteredUsers.stream().map(this::mapUserWithCities).toList();

        int start = page * limit;
        int end = Math.min(start + limit, filteredResponses.size());
        List<UserResponse> pageContent = start < filteredResponses.size()
                ? filteredResponses.subList(start, end)
                : List.of();
        Page<UserResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), filteredResponses.size());

        log.info("getUsers — requestingUserId: {}, getAll: {}, page: {}, limit: {}, filteredTotal: {}",
                requestingUserId, getAll, page, limit, filteredResponses.size());

        return UsersPageResponse.builder()
                .overallTotal(overallTotal)
                .activeCount(activeCount)
                .inactiveCount(inactiveCount)
                .countByRole(countByRole)
                .users(pageResult)
                .build();
    }

    private UserResponse mapUserWithCities(User user) {
        UserResponse response = userMapper.toResponse(user);
        if (user.getRole() == UserRole.LICENSEE) {
            List<LicenseeCity> cities = licenseeCityRepository.findByLicenseeId(user.getId());
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
    public UserResponse getUserDetail(Integer requestingUserId, Integer targetUserId) {
        log.debug("getUserDetail — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId));

        boolean isSelf = requestingUserId.equals(targetUserId);
        log.debug("getUserDetail — isSelf: {}, requesterRole: {}", isSelf, requestingUser.getRole());

        if (!isSelf) {
            switch (requestingUser.getRole()) {
                case LICENSEE:
                    if (!requestingUserId.equals(targetUser.getLicenseeId())) {
                        log.warn("getUserDetail — access denied — licenseeId: {} tried to view userId: {} (licenseeId: {})", requestingUserId, targetUserId, targetUser.getLicenseeId());
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                    }
                    break;
                case ADMIN:
                case SUPER_ADMIN:
                    break;
                default:
                    log.warn("getUserDetail — access denied — userId: {}, role: {}", requestingUserId, requestingUser.getRole());
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
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
        log.debug("updateUser — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId));

        boolean isSelf = requestingUserId.equals(targetUserId);
        UserRole requesterRole = requestingUser.getRole();
        boolean isAdmin = requesterRole == UserRole.ADMIN || requesterRole == UserRole.SUPER_ADMIN;

        log.debug("updateUser — isSelf: {}, requesterRole: {}, isAdmin: {}", isSelf, requesterRole, isAdmin);

        if (!isSelf && !isAdmin) {
            log.warn("updateUser — access denied — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        UserRole originalRole = targetUser.getRole();

        // Basic fields — any caller can update on an allowed target
        if (request.getFirstName() != null) targetUser.setFirstName(request.getFirstName());
        if (request.getLastName() != null) targetUser.setLastName(request.getLastName());
        if (request.getPhone() != null) targetUser.setPhone(request.getPhone());

        // Email change — check for uniqueness
        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(targetUserId))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
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
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin can only change role of Associates");
            } else if (requesterRole == UserRole.SUPER_ADMIN) {
                if (originalRole == UserRole.LICENSEE)
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Licensee role cannot be changed");
            }

            targetUser.setRole(request.getRole());

            if (request.getRole() != UserRole.ASSOCIATE) {
                targetUser.setLicenseeId(null);
            } else {
                if (request.getNewLicenseeId() != null) {
                    targetUser.setLicenseeId(request.getNewLicenseeId());
                } else if (targetUser.getLicenseeId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A licenseeId must be provided when setting role to ASSOCIATE");
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
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "City already exists: " + cityReq.getCity());
                    licenseeCityRepository.save(LicenseeCity.builder()
                            .licenseeId(targetUserId)
                            .city(cityReq.getCity())
                            .isPrimary(false)
                            .build());
                } else {
                    LicenseeCity toDelete = existingCities.stream()
                            .filter(c -> c.getCity().equalsIgnoreCase(cityReq.getCity()))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityReq.getCity()));
                    if (Boolean.TRUE.equals(toDelete.getIsPrimary()))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete primary city");
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
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No primary city found for licensee: " + targetUserId));

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

        UserRole updatedRole = savedUser.getRole();
        if (updatedRole == UserRole.ADMIN || updatedRole == UserRole.SUPER_ADMIN) {
            auditService.log(AuditActionType.ADMIN_UPDATED, RelatedEntityType.USER, targetUserId,
                    requestingUserId, null, auditService.snapshot(savedUser), null);
        }

        return userMapper.toResponse(savedUser);
    }

    @Override
    public String resetPassword(Integer requestingUserId, Integer targetUserId, ResetPasswordRequest request) {
        log.debug("resetPassword — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId));

        boolean isSelf = requestingUserId.equals(targetUserId);
        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;

        log.debug("resetPassword — isSelf: {}, isAdmin: {}", isSelf, isAdmin);

        if (!isSelf && !isAdmin) {
            log.warn("resetPassword — access denied — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (isSelf) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), targetUser.getPassword())) {
                log.warn("resetPassword — wrong current password — userId: {}", targetUserId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
            }
        }

        if (passwordEncoder.matches(request.getNewPassword(), targetUser.getPassword())) {
            log.warn("resetPassword — new password same as current — userId: {}", targetUserId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be same as current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("resetPassword — passwords do not match — userId: {}", targetUserId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        targetUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(targetUser);

        log.info("Password reset — targetId: {}, requestedBy: {}", targetUserId, requestingUserId);

        return "Password updated successfully";
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(Integer requestingUserId, Integer targetUserId) {
        log.debug("deactivateUser — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));

        UserRole requesterRole = requestingUser.getRole();
        if (requesterRole != UserRole.ADMIN && requesterRole != UserRole.SUPER_ADMIN) {
            log.warn("deactivateUser — access denied — requestingUserId: {}, role: {}", requestingUserId, requesterRole);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetUserId));

        log.debug("deactivateUser — target found — userId: {}, role: {}, status: {}", targetUserId, targetUser.getRole(), targetUser.getStatus());

        if (targetUser.getStatus() == UserStatus.INACTIVE) {
            log.warn("deactivateUser — already inactive — targetUserId: {}", targetUserId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already inactive");
        }

        UserRole targetRole = targetUser.getRole();

        if (requesterRole == UserRole.ADMIN && (targetRole == UserRole.ADMIN || targetRole == UserRole.SUPER_ADMIN)) {
            log.warn("deactivateUser — admin cannot deactivate another admin — requestingUserId: {}, targetUserId: {}", requestingUserId, targetUserId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin cannot deactivate another Admin");
        }

        switch (targetRole) {
            case ASSOCIATE -> {
                // TODO: transfer all prospects where associateId = targetUserId to associate's parent licensee — implement after ProspectService is built
                targetUser.setStatus(UserStatus.INACTIVE);
                resolveAlertIfPresent(AlertType.ASSOCIATE_DEACTIVATION_REQUEST, targetUserId);
            }
            case LICENSEE -> {
                targetUser.setStatus(UserStatus.INACTIVE);
                User mloUser = resolveMloUser();
                List<User> reassignedAssociates = reassignAssociatesToMlo(targetUserId, mloUser);
                List<Prospect> reassignedRecords = reassignProspectsAndClientsToMlo(targetUserId, mloUser);
                notifyMloReassignment(targetUser, mloUser, reassignedAssociates, reassignedRecords);
            }
            default -> targetUser.setStatus(UserStatus.INACTIVE);
        }

        userRepository.save(targetUser);

        log.info("User deactivated — targetId: {}, requestedBy: {}", targetUserId, requestingUserId);

        AuditActionType deactivateAction = switch (targetRole) {
            case ASSOCIATE -> AuditActionType.ASSOCIATE_DEACTIVATED;
            case LICENSEE -> AuditActionType.LICENSEE_DEACTIVATED;
            default -> AuditActionType.ADMIN_DEACTIVATED;
        };
        auditService.log(deactivateAction, RelatedEntityType.USER, targetUserId,
                requestingUserId, auditService.snapshot(targetUser), null, null);

        String fullName = targetUser.getFirstName() + " " + targetUser.getLastName();
        String role = targetUser.getRole().name();
        List<User> admins = new ArrayList<>();
        admins.addAll(userRepository.findByRole(UserRole.ADMIN));
        admins.addAll(userRepository.findByRole(UserRole.SUPER_ADMIN));
        admins.forEach(admin -> eventPublisher.publishEvent(new NotificationEvent(this,
                "User deactivated email — targetUserId: " + targetUserId + " — to: " + admin.getEmail(),
                ns -> ns.sendUserDeactivatedEmail(admin.getEmail(), fullName, role))));

        return userMapper.toResponse(targetUser);
    }

    private User resolveMloUser() {
        if (mloUserId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MLO licensee is not configured (app.mlo-user-id)");
        }
        return userRepository.findById(mloUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Configured MLO user not found with id: " + mloUserId));
    }

    private List<User> reassignAssociatesToMlo(Integer deactivatedLicenseeId, User mloUser) {
        List<User> associates = userRepository.findAssociatesByLicensee(deactivatedLicenseeId, UserRole.ASSOCIATE, null);
        associates.forEach(a -> a.setLicenseeId(mloUser.getId()));
        userRepository.saveAll(associates);

        log.info("reassignAssociatesToMlo — moved {} associate(s) from licenseeId: {} to MLO licenseeId: {}",
                associates.size(), deactivatedLicenseeId, mloUser.getId());
        return associates;
    }

    private List<Prospect> reassignProspectsAndClientsToMlo(Integer deactivatedLicenseeId, User mloUser) {
        List<ProspectLicensee> links = prospectLicenseeRepository.findByLicenseeId(deactivatedLicenseeId);
        links.forEach(l -> l.setLicenseeId(mloUser.getId()));
        prospectLicenseeRepository.saveAll(links);

        List<Integer> prospectIds = links.stream().map(ProspectLicensee::getProspectId).distinct().toList();
        List<Prospect> records = prospectIds.isEmpty()
                ? List.of()
                : prospectRepository.findByIdInAndDeletionStatusFalse(prospectIds);

        log.info("reassignProspectsAndClientsToMlo — moved {} prospect/client link(s) ({} record(s)) from licenseeId: {} to MLO licenseeId: {}",
                links.size(), records.size(), deactivatedLicenseeId, mloUser.getId());
        return records;
    }

    private void notifyMloReassignment(User deactivatedLicensee, User mloUser, List<User> reassignedAssociates, List<Prospect> reassignedRecords) {
        String licenseeName = deactivatedLicensee.getFirstName() + " " + deactivatedLicensee.getLastName();
        String associateSummary = buildAssociateSummaryTable(reassignedAssociates);
        String prospectSummary = buildProspectSummaryTable(reassignedRecords);

        List<User> recipients = new ArrayList<>();
        recipients.addAll(userRepository.findByRole(UserRole.ADMIN));
        recipients.addAll(userRepository.findByRole(UserRole.SUPER_ADMIN));
        recipients.add(mloUser);

        recipients.forEach(recipient -> eventPublisher.publishEvent(new NotificationEvent(this,
                "Licensee reassignment summary email — deactivatedLicenseeId: " + deactivatedLicensee.getId() + " — to: " + recipient.getEmail(),
                ns -> ns.sendLicenseeReassignmentSummaryEmail(recipient.getEmail(), licenseeName, associateSummary, prospectSummary))));
    }

    private String buildAssociateSummaryTable(List<User> associates) {
        if (associates.isEmpty()) {
            return "No associates were reassigned.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total reassigned: %d%n%n", associates.size()));
        sb.append(String.format("%-25s %-35s%n", "Name", "Email"));
        sb.append("-".repeat(60)).append("\n");
        for (User associate : associates) {
            sb.append(String.format("%-25s %-35s%n", associate.getFirstName() + " " + associate.getLastName(), associate.getEmail()));
        }
        return sb.toString();
    }

    private String buildProspectSummaryTable(List<Prospect> records) {
        if (records.isEmpty()) {
            return "No prospects or clients were reassigned.";
        }
        long prospectCount = records.stream().filter(r -> r.getType() == ProspectType.PROSPECT).count();
        long clientCount = records.stream().filter(r -> r.getType() == ProspectType.CLIENT).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total reassigned: %d (Prospects: %d, Clients: %d)%n%n", records.size(), prospectCount, clientCount));
        sb.append(String.format("%-30s %-10s %-20s%n", "Company Name", "Type", "City"));
        sb.append("-".repeat(62)).append("\n");
        for (Prospect record : records) {
            sb.append(String.format("%-30s %-10s %-20s%n", record.getCompanyName(), record.getType(), record.getCity()));
        }
        return sb.toString();
    }

    @Override
    public String requestAssociateDeactivation(Integer requestingLicenseeId, Integer targetAssociateId) {
        log.debug("requestAssociateDeactivation — requestingLicenseeId: {}, targetAssociateId: {}", requestingLicenseeId, targetAssociateId);

        User requestingUser = userRepository.findById(requestingLicenseeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingLicenseeId));
        if (requestingUser.getRole() != UserRole.LICENSEE) {
            log.warn("requestAssociateDeactivation — access denied — userId: {}, role: {}", requestingLicenseeId, requestingUser.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        User targetUser = userRepository.findById(targetAssociateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + targetAssociateId));
        if (targetUser.getRole() != UserRole.ASSOCIATE) {
            log.warn("requestAssociateDeactivation — target is not associate — targetUserId: {}, role: {}", targetAssociateId, targetUser.getRole());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not an Associate");
        }
        if (!requestingLicenseeId.equals(targetUser.getLicenseeId())) {
            log.warn("requestAssociateDeactivation — associate does not belong to licensee — licenseeId: {}, associateId: {}, associateLicenseeId: {}", requestingLicenseeId, targetAssociateId, targetUser.getLicenseeId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This Associate does not belong to your licensee");
        }
        if (targetUser.getStatus() == UserStatus.INACTIVE) {
            log.warn("requestAssociateDeactivation — already inactive — targetAssociateId: {}", targetAssociateId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already inactive");
        }

        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(
                AlertType.ASSOCIATE_DEACTIVATION_REQUEST, targetAssociateId, AlertStatus.PENDING
        ).ifPresent(a -> {
            log.warn("requestAssociateDeactivation — duplicate request — targetAssociateId: {}", targetAssociateId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A deactivation request for this Associate is already pending");
        });

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
        log.debug("approveRejectAssociateDeactivation — alertId: {}, approve: {}, requestingUserId: {}", alertId, approve, requestingUserId);

        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            log.warn("approveRejectAssociateDeactivation — access denied — userId: {}, role: {}", requestingUserId, requestingUser.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        if (alert.getAlertType() != AlertType.ASSOCIATE_DEACTIVATION_REQUEST) {
            log.warn("approveRejectAssociateDeactivation — wrong alert type — alertId: {}, type: {}", alertId, alert.getAlertType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert is not of type ASSOCIATE_DEACTIVATION_REQUEST");
        }
        if (alert.getStatus() != AlertStatus.PENDING) {
            log.warn("approveRejectAssociateDeactivation — alert no longer pending — alertId: {}, status: {}", alertId, alert.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alert is no longer pending");
        }

        if (!approve) {
            alert.setStatus(AlertStatus.REJECTED);
            alertRepository.save(alert);
            log.info("approveRejectAssociateDeactivation — rejected — alertId: {}, rejectedBy: {}", alertId, requestingUserId);
            auditService.log(AuditActionType.ASSOCIATE_REQUEST_REJECTED, RelatedEntityType.USER,
                    alert.getRelatedEntityId(), requestingUserId, null, null, Map.of("alertId", alertId, "requestType", "DEACTIVATION"));
            return ApiResponse.success("Associate deactivation request rejected", null);
        }

        log.debug("approveRejectAssociateDeactivation — approving — alertId: {}, associateId: {}", alertId, alert.getRelatedEntityId());

        alert.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(alert);

        UserResponse response = deactivateUser(requestingUserId, alert.getRelatedEntityId());

        log.info("Associate deactivation approved — associateId: {}, approvedBy: {}", alert.getRelatedEntityId(), requestingUserId);

        return ApiResponse.success("Associate deactivated successfully", response);
    }

    @Override
    @Transactional(readOnly = true)
    public UsersPageResponse searchUsers(Integer requestingUserId, String q, String scope, UserRole role, int page, int limit) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + requestingUserId));

        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;
        boolean scopeAll = "all".equalsIgnoreCase(scope) || isAdmin;
        String keyword = "%" + q.trim() + "%";

        List<User> users;
        if (scopeAll) {
            users = userRepository.searchAll(keyword);
        } else {
            Integer licenseeId;
            if (requestingUser.getRole() == UserRole.ASSOCIATE) {
                licenseeId = requestingUser.getLicenseeId();
            } else if (requestingUser.getRole() == UserRole.LICENSEE) {
                licenseeId = requestingUserId;
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            users = userRepository.searchByLicenseeId(keyword, licenseeId);
        }

        if (role != null) {
            users = users.stream().filter(u -> u.getRole() == role).toList();
        }

        long overallTotal = users.size();
        long activeCount = users.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        long inactiveCount = users.stream().filter(u -> u.getStatus() == UserStatus.INACTIVE).count();
        Map<UserRole, Long> countByRole = users.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));

        List<UserResponse> allResponses = users.stream().map(this::mapUserWithCities).toList();

        int start = page * limit;
        int end = Math.min(start + limit, allResponses.size());
        List<UserResponse> pageContent = start < allResponses.size()
                ? allResponses.subList(start, end) : List.of();
        Page<UserResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), allResponses.size());

        log.info("searchUsers — requestingUserId: {}, scope: {}, role: {}, total: {}", requestingUserId, scope, role, overallTotal);

        return UsersPageResponse.builder()
                .overallTotal(overallTotal)
                .activeCount(activeCount)
                .inactiveCount(inactiveCount)
                .countByRole(countByRole)
                .users(pageResult)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsersPageResponse getLicenseesAndAssociates(Integer requestingUserId, List<UserRole> roles, Integer licenseeId) {
        List<UserRole> effectiveRoles = (roles == null || roles.isEmpty())
                ? List.of(UserRole.values())
                : roles;
        List<User> users = userRepository.findActiveByRolesAndLicenseeId(effectiveRoles, UserStatus.ACTIVE, licenseeId);

        long overallTotal = users.size();
        long activeCount = users.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        long inactiveCount = users.stream().filter(u -> u.getStatus() == UserStatus.INACTIVE).count();
        Map<UserRole, Long> countByRole = Arrays.stream(UserRole.values())
                .collect(Collectors.toMap(r -> r, r -> users.stream().filter(u -> u.getRole() == r).count()));

        List<UserResponse> allResponses = users.stream().map(this::mapUserWithCities).toList();
        Page<UserResponse> pageResult = new PageImpl<>(allResponses);

        log.info("getLicenseesAndAssociates — requestingUserId: {}, roles: {}, licenseeId: {}, total: {}",
                requestingUserId, effectiveRoles, licenseeId, overallTotal);

        return UsersPageResponse.builder()
                .overallTotal(overallTotal)
                .activeCount(activeCount)
                .inactiveCount(inactiveCount)
                .countByRole(countByRole)
                .users(pageResult)
                .build();
    }

    private void resolveAlertIfPresent(AlertType alertType, Integer relatedEntityId) {
        alertRepository.findByAlertTypeAndRelatedEntityIdAndStatus(alertType, relatedEntityId, AlertStatus.PENDING)
                .ifPresent(a -> {
                    a.setStatus(AlertStatus.RESOLVED);
                    alertRepository.save(a);
                    log.info("Alert auto-resolved — alertId: {}, type: {}, relatedEntityId: {}", a.getId(), alertType, relatedEntityId);
                });
    }

    @Override
    @Transactional
    public ImportResult importUsers(MultipartFile file, Integer requestingUserId) {
        log.info("importUsers — started — requestingUserId: {}, filename: {}", requestingUserId, file.getOriginalFilename());

        List<String[]> licenseeRows = new ArrayList<>();
        List<String[]> associateRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String role = cellString(row.getCell(0));
                if (role == null || role.isBlank()) continue;

                totalRows++;
                String[] data = new String[]{
                        role.trim().toUpperCase(),
                        cellString(row.getCell(1)),  // firstName
                        cellString(row.getCell(2)),  // lastName
                        cellString(row.getCell(3)),  // email
                        cellPhone(row.getCell(4)),   // phone (may be numeric in Excel)
                        cellString(row.getCell(5)),  // cities
                        cellString(row.getCell(6)),  // primaryCity
                        cellString(row.getCell(7))   // licenseeEmail
                };

                if ("LICENSEE".equals(data[0])) licenseeRows.add(data);
                else if ("ASSOCIATE".equals(data[0])) associateRows.add(data);
                else errors.add("Row " + (i + 1) + ": unknown role '" + data[0] + "' — skipped");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read Excel file: " + e.getMessage());
        }

        int imported = 0;
        int skipped = 0;

        // email → userId map for licensees created in this batch (to resolve associate references)
        Map<String, Integer> importedLicenseeIds = new HashMap<>();

        for (String[] row : licenseeRows) {
            String email = row[3] != null ? row[3].trim().toLowerCase() : null;
            if (email == null || email.isBlank()) {
                errors.add("Licensee '" + row[1] + " " + row[2] + "': missing email — skipped");
                skipped++;
                continue;
            }
            if (userRepository.findByEmail(email).isPresent()) {
                log.warn("importUsers — DUPLICATE licensee skipped (email exists): {}", email);
                // still register the ID so associates in this file can link to existing licensees
                userRepository.findByEmail(email).ifPresent(u -> importedLicenseeIds.put(email, u.getId()));
                skipped++;
                continue;
            }

            String phone = row[4];
            if (phone != null && userRepository.findByPhone(phone).isPresent()) {
                errors.add("Licensee '" + email + "': phone " + phone + " already exists — skipped");
                skipped++;
                continue;
            }

            String citiesRaw = row[5];
            String primaryCity = row[6] != null ? row[6].trim() : null;
            List<String> cityNames = (citiesRaw != null && !citiesRaw.isBlank())
                    ? Arrays.stream(citiesRaw.split(",")).map(String::trim).filter(c -> !c.isBlank()).toList()
                    : List.of();

            if (cityNames.isEmpty()) {
                errors.add("Licensee '" + email + "': missing cities — skipped");
                skipped++;
                continue;
            }

            if (primaryCity == null || primaryCity.isBlank()) {
                if (cityNames.size() == 1) {
                    primaryCity = cityNames.get(0);
                    log.info("importUsers — single city defaulted as primary for licensee: {} — city: {}", email, primaryCity);
                } else {
                    errors.add("Licensee '" + email + "': multiple cities but no primaryCity specified — skipped");
                    skipped++;
                    continue;
                }
            }

            String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String invitationToken = UUID.randomUUID().toString();

            User user = User.builder()
                    .firstName(row[1] != null ? row[1].trim() : "")
                    .lastName(row[2] != null ? row[2].trim() : "")
                    .email(email)
                    .phone(phone)
                    .password(passwordEncoder.encode(tempPassword))
                    .role(UserRole.LICENSEE)
                    .status(UserStatus.PENDING)
                    .invitationToken(invitationToken)
                    .build();
            user = userRepository.save(user);
            final Integer userId = user.getId();

            final String finalPrimary = primaryCity;
            List<LicenseeCity> cities = cityNames.stream()
                    .map(c -> LicenseeCity.builder()
                            .licenseeId(userId)
                            .city(c)
                            .isPrimary(c.equalsIgnoreCase(finalPrimary))
                            .build())
                    .toList();
            licenseeCityRepository.saveAll(cities);

            importedLicenseeIds.put(email, userId);

            log.info("importUsers — licensee imported — id: {}, email: {}", userId, email);
            imported++;
        }

        for (String[] row : associateRows) {
            String email = row[3] != null ? row[3].trim().toLowerCase() : null;
            if (email == null || email.isBlank()) {
                errors.add("Associate '" + row[1] + " " + row[2] + "': missing email — skipped");
                skipped++;
                continue;
            }
            if (userRepository.findByEmail(email).isPresent()) {
                log.warn("importUsers — DUPLICATE associate skipped (email exists): {}", email);
                skipped++;
                continue;
            }

            String phone = row[4];
            if (phone != null && userRepository.findByPhone(phone).isPresent()) {
                errors.add("Associate '" + email + "': phone " + phone + " already exists — skipped");
                skipped++;
                continue;
            }

            String licenseeEmail = row[7] != null ? row[7].trim().toLowerCase() : null;
            if (licenseeEmail == null || licenseeEmail.isBlank()) {
                errors.add("Associate '" + email + "': missing licenseeEmail — skipped");
                skipped++;
                continue;
            }

            Integer licenseeId = importedLicenseeIds.get(licenseeEmail);
            if (licenseeId == null) {
                licenseeId = userRepository.findByEmail(licenseeEmail)
                        .filter(u -> u.getRole() == UserRole.LICENSEE)
                        .map(User::getId)
                        .orElse(null);
            }
            if (licenseeId == null) {
                errors.add("Associate '" + email + "': licensee '" + licenseeEmail + "' not found — skipped");
                skipped++;
                continue;
            }

            String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String invitationToken = UUID.randomUUID().toString();

            User associate = User.builder()
                    .firstName(row[1] != null ? row[1].trim() : "")
                    .lastName(row[2] != null ? row[2].trim() : "")
                    .email(email)
                    .phone(phone)
                    .password(passwordEncoder.encode(tempPassword))
                    .role(UserRole.ASSOCIATE)
                    .status(UserStatus.PENDING)
                    .invitationToken(invitationToken)
                    .licenseeId(licenseeId)
                    .build();
            associate = userRepository.save(associate);

            log.info("importUsers — associate imported — id: {}, email: {}, licenseeId: {}", associate.getId(), email, licenseeId);
            imported++;
        }

        log.info("importUsers — complete — total: {}, imported: {}, skipped: {}, errors: {}", totalRows, imported, skipped, errors.size());

        return ImportResult.builder()
                .totalRows(totalRows)
                .imported(imported)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional
    public ImportResult sendInvites(List<Integer> userIds, Integer requestingUserId) {
        log.info("sendInvites — requestingUserId: {}, userIds: {}", requestingUserId, userIds);

        List<String> errors = new ArrayList<>();
        int sent = 0;
        int skipped = 0;

        for (Integer userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                errors.add("User id " + userId + ": not found — skipped");
                skipped++;
                continue;
            }
            if (user.getStatus() != UserStatus.PENDING) {
                errors.add("User id " + userId + " (" + user.getEmail() + "): status is " + user.getStatus() + ", expected PENDING — skipped");
                skipped++;
                continue;
            }

            dispatchInvite(user);
            sent++;
        }

        log.info("sendInvites — complete — sent: {}, skipped: {}", sent, skipped);
        return ImportResult.builder()
                .totalRows(userIds.size())
                .imported(sent)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional
    public ImportResult sendInvitesAll(Integer requestingUserId) {
        List<User> pending = userRepository.findByInviteEmailSentFalseAndStatus(UserStatus.PENDING);
        log.info("sendInvitesAll — requestingUserId: {}, eligible users: {}", requestingUserId, pending.size());

        for (User user : pending) {
            dispatchInvite(user);
        }

        log.info("sendInvitesAll — complete — sent: {}", pending.size());
        return ImportResult.builder()
                .totalRows(pending.size())
                .imported(pending.size())
                .skipped(0)
                .errors(List.of())
                .build();
    }

    private void dispatchInvite(User user) {
        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String invitationToken = UUID.randomUUID().toString();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setInvitationToken(invitationToken);
        user.setInviteEmailSent(true);
        userRepository.save(user);

        final String email = user.getEmail();
        final String inviteLink = frontendUrl + "/setup-account?token=" + invitationToken;
        eventPublisher.publishEvent(new NotificationEvent(this, "Invite email — userId: " + user.getId(),
                ns -> ns.sendInviteEmail(email, inviteLink, tempPassword)));

        log.info("dispatchInvite — sent — userId: {}, email: {}", user.getId(), email);
    }

    private String cellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private String cellPhone(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            long val = (long) cell.getNumericCellValue();
            return String.valueOf(val);
        }
        String raw = cellString(cell);
        return raw != null ? raw.trim() : null;
    }
}
