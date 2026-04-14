package com.lmi.crm.entity;

import com.lmi.crm.enums.DeliveryType;
import com.lmi.crm.enums.GroupProgramType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "groups")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "licensee_id", nullable = false)
    private Integer licenseeId;

    @Column(name = "facilitator_id")
    private Integer facilitatorId;

    @Column(name = "group_size")
    private Integer groupSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    private GroupProgramType groupType;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type")
    private DeliveryType deliveryType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "ppm_tfe_date_sent")
    private LocalDate ppmTfeDateSent;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "deletion_status")
    private Boolean deletionStatus = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
