package com.lmi.crm.entity;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProtectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prospects", indexes = {
        @Index(columnList = "company_name, city"),
        @Index(columnList = "associate_id"),
        @Index(columnList = "protection_status"),
        @Index(columnList = "type"),
        @Index(columnList = "created_at")
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Prospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company_name")
    private String companyName;

    private String city;

    @Column(name = "contact_first_name")
    private String contactFirstName;

    @Column(name = "contact_last_name")
    private String contactLastName;

    private String designation;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(name = "referred_by")
    private String referredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_type")
    private ClassificationType classificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type")
    private ProspectProgramType programType;

    @Enumerated(EnumType.STRING)
    private ProspectType type;

    @Column(name = "associate_id")
    private Integer associateId;

    @Column(name = "first_meeting_date")
    private LocalDate firstMeetingDate;

    @Column(name = "last_meeting_date")
    private LocalDate lastMeetingDate;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_status")
    private ProtectionStatus protectionStatus;

    @Column(name = "protection_period_months")
    private Integer protectionPeriodMonths;

    @Column(name = "deletion_status")
    private Boolean deletionStatus = false;

    @Column(name = "created_by")
    private Integer createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
