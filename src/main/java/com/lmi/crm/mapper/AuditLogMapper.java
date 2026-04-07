package com.lmi.crm.mapper;

import com.lmi.crm.dto.AuditLogRequestDTO;
import com.lmi.crm.dto.AuditLogResponseDTO;
import com.lmi.crm.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLog toEntity(AuditLogRequestDTO dto) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(dto.getActionType());
        auditLog.setEntityType(dto.getEntityType());
        auditLog.setEntityId(dto.getEntityId());
        auditLog.setPerformedBy(dto.getPerformedBy());
        auditLog.setPreviousState(dto.getPreviousState());
        auditLog.setNewState(dto.getNewState());
        auditLog.setMetadata(dto.getMetadata());
        return auditLog;
    }

    public void updateEntity(AuditLog auditLog, AuditLogRequestDTO dto) {
        auditLog.setActionType(dto.getActionType());
        auditLog.setEntityType(dto.getEntityType());
        auditLog.setEntityId(dto.getEntityId());
        auditLog.setPerformedBy(dto.getPerformedBy());
        auditLog.setPreviousState(dto.getPreviousState());
        auditLog.setNewState(dto.getNewState());
        auditLog.setMetadata(dto.getMetadata());
    }

    public AuditLogResponseDTO toDTO(AuditLog auditLog) {
        AuditLogResponseDTO dto = new AuditLogResponseDTO();
        dto.setId(auditLog.getId());
        dto.setActionType(auditLog.getActionType());
        dto.setEntityType(auditLog.getEntityType());
        dto.setEntityId(auditLog.getEntityId());
        dto.setPerformedBy(auditLog.getPerformedBy());
        dto.setPreviousState(auditLog.getPreviousState());
        dto.setNewState(auditLog.getNewState());
        dto.setMetadata(auditLog.getMetadata());
        dto.setCreatedAt(auditLog.getCreatedAt());
        return dto;
    }
}
