package com.lmi.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.request.RequestAssociateCreationRequest;
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
    public UserResponse approveRejectAssociateCreation(Integer alertId, boolean approve, Integer requestingAdminId) {
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
            return null;
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

        return userMapper.toResponse(associate);
    }
}
