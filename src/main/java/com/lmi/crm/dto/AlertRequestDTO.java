package com.lmi.crm.dto;

import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.Data;

@Data
public class AlertRequestDTO {
    private AlertType alertType;
    private String title;
    private String description;
    private RelatedEntityType relatedEntityType;
    private Integer relatedEntityId;
    private Integer triggeredBy;
    private Boolean actionRequired;
}
