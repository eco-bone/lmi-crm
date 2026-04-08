package com.lmi.crm.service;

import com.lmi.crm.dao.AlertRepository;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void createAlert(AlertType alertType, String title, String description,
                            RelatedEntityType relatedEntityType, Integer relatedEntityId,
                            Integer triggeredBy, boolean actionRequired) {

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

        alertRepository.save(alert);

        log.info("Alert created — type: {}, title: {}, triggeredBy: {}", alertType, title, triggeredBy);

        notificationService.sendAdminAlertEmail(title, description, baseUrl + "/admin/alerts");
    }
}
