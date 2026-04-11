package com.lmi.crm.entity;

import com.lmi.crm.enums.OtpType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String otp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
