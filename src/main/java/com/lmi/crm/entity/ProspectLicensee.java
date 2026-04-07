package com.lmi.crm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prospect_licensees", indexes = {
        @Index(columnList = "prospect_id"),
        @Index(columnList = "licensee_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"prospect_id", "licensee_id"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProspectLicensee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "prospect_id", nullable = false)
    private Integer prospectId;

    @Column(name = "licensee_id", nullable = false)
    private Integer licenseeId;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}
