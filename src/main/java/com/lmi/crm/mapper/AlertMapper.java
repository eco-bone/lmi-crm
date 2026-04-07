package com.lmi.crm.mapper;

import com.lmi.crm.dto.AlertRequestDTO;
import com.lmi.crm.dto.AlertResponseDTO;
import com.lmi.crm.entity.Alert;
import com.lmi.crm.enums.AlertStatus;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public Alert toEntity(AlertRequestDTO dto) {
        Alert alert = new Alert();
        alert.setAlertType(dto.getAlertType());
        alert.setTitle(dto.getTitle());
        alert.setDescription(dto.getDescription());
        alert.setRelatedEntityType(dto.getRelatedEntityType());
        alert.setRelatedEntityId(dto.getRelatedEntityId());
        alert.setTriggeredBy(dto.getTriggeredBy());
        alert.setActionRequired(dto.getActionRequired());
        alert.setStatus(AlertStatus.PENDING);
        return alert;
    }

    public void updateEntity(Alert alert, AlertRequestDTO dto) {
        alert.setAlertType(dto.getAlertType());
        alert.setTitle(dto.getTitle());
        alert.setDescription(dto.getDescription());
        alert.setRelatedEntityType(dto.getRelatedEntityType());
        alert.setRelatedEntityId(dto.getRelatedEntityId());
        alert.setTriggeredBy(dto.getTriggeredBy());
        alert.setActionRequired(dto.getActionRequired());
    }

    public AlertResponseDTO toDTO(Alert alert) {
        AlertResponseDTO dto = new AlertResponseDTO();
        dto.setId(alert.getId());
        dto.setAlertType(alert.getAlertType());
        dto.setTitle(alert.getTitle());
        dto.setDescription(alert.getDescription());
        dto.setRelatedEntityType(alert.getRelatedEntityType());
        dto.setRelatedEntityId(alert.getRelatedEntityId());
        dto.setTriggeredBy(alert.getTriggeredBy());
        dto.setStatus(alert.getStatus());
        dto.setActionRequired(alert.getActionRequired());
        dto.setCreatedAt(alert.getCreatedAt());
        return dto;
    }
}
