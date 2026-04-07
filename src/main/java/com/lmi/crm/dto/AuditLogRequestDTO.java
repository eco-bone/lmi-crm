package com.lmi.crm.dto;

import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.Data;

import java.util.Map;

@Data
public class AuditLogRequestDTO {
    private AuditActionType actionType;
    private RelatedEntityType entityType;
    private Integer entityId;
    private Integer performedBy;
    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private Map<String, Object> metadata;
}
