package com.lmi.crm.dto.response;

import com.lmi.crm.enums.AlertStatus;
import com.lmi.crm.enums.AlertType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private Integer id;
    private AlertType alertType;
    private String title;
    private String description;
    private RelatedEntityType relatedEntityType;
    private Integer relatedEntityId;
    private Integer triggeredBy;
    private String triggeredByName;
    private AlertStatus status;
    private boolean actionRequired;
    private LocalDateTime createdAt;
}
