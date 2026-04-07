package com.lmi.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_prospects", indexes = {
        @Index(columnList = "group_id"),
        @Index(columnList = "prospect_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"group_id", "prospect_id"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupProspect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;

    @Column(name = "prospect_id", nullable = false)
    private Integer prospectId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
