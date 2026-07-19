package com.doliuw.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store", indexes = {
    @Index(name = "idx_otp_mobile", columnList = "mobile"),
    @Index(name = "idx_otp_expiry", columnList = "expiry_time")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @Column
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
