package com.lmi.crm.entity;

import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.RelatedEntityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(columnList = "entity_type"),
        @Index(columnList = "entity_id"),
        @Index(columnList = "performed_by"),
        @Index(columnList = "created_at")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private RelatedEntityType entityType;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "performed_by")
    private Integer performedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    private Map<String, Object> previousState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", columnDefinition = "jsonb")
    private Map<String, Object> newState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
