package com.lmi.crm.dto;

import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertResponseDTO {
    private Integer id;
    private AlertType alertType;
    private String title;
    private String description;
    private RelatedEntityType relatedEntityType;
    private Integer relatedEntityId;
    private Integer triggeredBy;
    private AlertStatus status;
    private Boolean actionRequired;
    private LocalDateTime createdAt;
}
