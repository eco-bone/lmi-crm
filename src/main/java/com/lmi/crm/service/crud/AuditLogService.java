package com.lmi.crm.service.crud;

import com.lmi.crm.dto.AuditLogRequestDTO;
import com.lmi.crm.dto.AuditLogResponseDTO;

import java.util.List;

public interface AuditLogService {
    AuditLogResponseDTO createAuditLog(AuditLogRequestDTO request);
    AuditLogResponseDTO getAuditLogById(Integer id);
    List<AuditLogResponseDTO> getAllAuditLogs();
    AuditLogResponseDTO updateAuditLog(Integer id, AuditLogRequestDTO request);
    void deleteAuditLog(Integer id);
}
