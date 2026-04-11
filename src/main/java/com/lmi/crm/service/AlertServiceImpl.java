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
}
