package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.response.AlertResponse;
import com.lmi.crm.dto.response.AlertSummaryResponse;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.AlertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private NotificationService notificationService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void createAlert(AlertType alertType, String title, String description,
                            RelatedEntityType relatedEntityType, Integer relatedEntityId,
                            Integer triggeredBy, boolean actionRequired) {

        log.debug("createAlert — type: {}, relatedEntityType: {}, relatedEntityId: {}, triggeredBy: {}, actionRequired: {}",
                alertType, relatedEntityType, relatedEntityId, triggeredBy, actionRequired);

        Alert alert = Alert.builder()
                .alertType(alertType)
                .title(title)
                .description(description)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .triggeredBy(triggeredBy)
                .status(AlertStatus.PENDING)
                .actionRequired(actionRequired)
                .build();

        Alert saved = alertRepository.save(alert);
        log.info("createAlert — saved — alertId: {}, type: {}, title: {}, triggeredBy: {}", saved.getId(), alertType, title, triggeredBy);

        log.debug("createAlert — dispatching admin email notification — alertId: {}", saved.getId());
        notificationService.sendAdminAlertEmail(title, description, baseUrl + "/admin/alerts");
    }

    @Override
    public Page<AlertResponse> getAlerts(Integer requestingUserId, AlertType typeFilter, AlertStatus statusFilter, int page, int size) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Alert> alerts;
        if (typeFilter != null && statusFilter != null) {
            alerts = alertRepository.findByAlertTypeAndStatus(typeFilter, statusFilter, pageable);
        } else if (typeFilter != null) {
            alerts = alertRepository.findByAlertType(typeFilter, pageable);
        } else if (statusFilter != null) {
            alerts = alertRepository.findByStatus(statusFilter, pageable);
        } else {
            alerts = alertRepository.findAll(pageable);
        }

        log.info("GET /api/admin/alerts — requestingUserId: {}, typeFilter: {}, statusFilter: {}, page: {}, size: {}", requestingUserId, typeFilter, statusFilter, page, size);
        log.info("GET /api/admin/alerts — returning {} total alerts", alerts.getTotalElements());

        return alerts.map(alert -> {
            String name = null;
            if (alert.getTriggeredBy() != null) {
                name = userRepository.findById(alert.getTriggeredBy())
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse("Unknown");
            }
            return alertMapper.toResponse(alert, name);
        });
    }

    @Override
    public AlertSummaryResponse getAlertSummary(Integer requestingUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        List<Object[]> rows = alertRepository.countByAlertType();
        Map<AlertType, Long> byType = Arrays.stream(AlertType.values())
                .collect(Collectors.toMap(t -> t, t -> 0L));
        rows.forEach(row -> byType.put((AlertType) row[0], (Long) row[1]));

        long total = byType.values().stream().mapToLong(Long::longValue).sum();

        log.info("getAlertSummary — requestingUserId: {}, total: {}", requestingUserId, total);

        return new AlertSummaryResponse(total, byType);
    }

    @Override
    public AlertResponse getAlertDetail(Integer requestingUserId, Integer alertId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requestingUser.getRole() != UserRole.ADMIN && requestingUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new RuntimeException("Access denied");
        }

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        log.info("GET /api/admin/alerts/{} — requestingUserId: {}", alertId, requestingUserId);

        String name = null;
        if (alert.getTriggeredBy() != null) {
            name = userRepository.findById(alert.getTriggeredBy())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Unknown");
        }
        return alertMapper.toResponse(alert, name);
    }
}
