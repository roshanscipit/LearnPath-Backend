package com.doliuw.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bookings_user", columnList = "user_id"),
    @Index(name = "idx_bookings_date", columnList = "booking_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column
    private String paymentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
}
