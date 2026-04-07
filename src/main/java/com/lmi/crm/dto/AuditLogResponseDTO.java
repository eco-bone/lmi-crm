package com.lmi.crm.dto;

import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AuditLogResponseDTO {
    private Integer id;
    private AuditActionType actionType;
    private RelatedEntityType entityType;
    private Integer entityId;
    private Integer performedBy;
    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
