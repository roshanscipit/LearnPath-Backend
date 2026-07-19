package com.doliuw.entity;

import com.doliuw.config.FieldEncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_mobile", columnList = "mobile")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ENCRYPTED: only id and email are stored in plaintext
    // A DB breach exposes only those two fields — everything else is AES-256-GCM encrypted
    @Convert(converter = FieldEncryptionConverter.class)
    @Column(nullable = false)
    private String name;

    @Column(unique = true)   // email stays plaintext (needed for login lookups / indexes)
    private String email;

    @Column(unique = true)
    private String mobile;   // stored as hash in DB index; use encrypted name lookup by app

    @Column
    private String password; // already BCrypt-hashed by Spring Security

    @Convert(converter = FieldEncryptionConverter.class)
    @Column
    private String avatar;   // encrypted URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.EMAIL;

    @Column
    private String googleId;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProgress progress;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    public enum AuthProvider {
        EMAIL, MOBILE, GOOGLE
    }
}
