package com.lmi.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meetings", indexes = {
        @Index(columnList = "prospect_id, meeting_at")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "prospect_id", nullable = false)
    private Integer prospectId;

    @Column(name = "point_of_contact")
    private String pointOfContact;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meeting_at")
    private LocalDateTime meetingAt;

    @Column(name = "created_by")
    private Integer createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
