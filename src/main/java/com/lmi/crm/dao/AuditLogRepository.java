package com.lmi.crm.dao;

import com.lmi.crm.entity.AuditLog;
import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.RelatedEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(RelatedEntityType entityType, Integer entityId);

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(RelatedEntityType entityType, Pageable pageable);

    Page<AuditLog> findByActionTypeOrderByCreatedAtDesc(AuditActionType actionType, Pageable pageable);

    Page<AuditLog> findByPerformedByOrderByCreatedAtDesc(Integer performedBy, Pageable pageable);
}
