package com.lmi.crm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "licensee_cities", indexes = {
        @Index(columnList = "licensee_id"),
        @Index(columnList = "city")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"licensee_id", "city", "is_primary"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LicenseeCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "licensee_id", nullable = false)
    private Integer licenseeId;

    @Column(nullable = false)
    private String city;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
