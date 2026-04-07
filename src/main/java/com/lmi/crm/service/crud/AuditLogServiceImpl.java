package com.lmi.crm.service.crud;

import com.lmi.crm.dao.AuditLogRepository;
import com.lmi.crm.dto.AuditLogRequestDTO;
import com.lmi.crm.dto.AuditLogResponseDTO;
import com.lmi.crm.entity.AuditLog;
import com.lmi.crm.mapper.AuditLogMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Override
    public AuditLogResponseDTO createAuditLog(AuditLogRequestDTO request) {
        log.info("Creating audit log for action {}", request.getActionType());
        AuditLog auditLog = auditLogMapper.toEntity(request);
        return auditLogMapper.toDTO(auditLogRepository.save(auditLog));
    }

    @Override
    public AuditLogResponseDTO getAuditLogById(Integer id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("AuditLog with ID {} not found", id);
                    return new EntityNotFoundException("AuditLog not found with ID: " + id);
                });
        return auditLogMapper.toDTO(auditLog);
    }

    @Override
    public List<AuditLogResponseDTO> getAllAuditLogs() {
        return auditLogRepository.findAll()
                .stream()
                .map(auditLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AuditLogResponseDTO updateAuditLog(Integer id, AuditLogRequestDTO request) {
        log.info("Updating audit log with ID {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("AuditLog with ID {} not found", id);
                    return new EntityNotFoundException("AuditLog not found with ID: " + id);
                });
        auditLogMapper.updateEntity(auditLog, request);
        return auditLogMapper.toDTO(auditLogRepository.save(auditLog));
    }

    @Override
    public void deleteAuditLog(Integer id) {
        log.info("Deleting audit log with ID {}", id);
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("AuditLog with ID {} not found", id);
                    return new EntityNotFoundException("AuditLog not found with ID: " + id);
                });
        auditLogRepository.delete(auditLog);
    }
}
