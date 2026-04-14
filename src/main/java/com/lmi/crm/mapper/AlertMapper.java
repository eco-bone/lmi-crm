package com.lmi.crm.mapper;

import com.lmi.crm.dto.response.AlertResponse;
import com.lmi.crm.entity.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .relatedEntityType(alert.getRelatedEntityType())
                .relatedEntityId(alert.getRelatedEntityId())
                .triggeredBy(alert.getTriggeredBy())
                .status(alert.getStatus())
                .actionRequired(Boolean.TRUE.equals(alert.getActionRequired()))
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
