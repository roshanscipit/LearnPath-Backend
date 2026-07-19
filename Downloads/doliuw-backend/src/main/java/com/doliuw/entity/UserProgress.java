package com.doliuw.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    @Builder.Default
    private String selectedRole = "java";

    @Column
    @Builder.Default
    private String selectedVariant = "Full Stack";

    // Stored as JSON string: e.g. ["aptitude","coding"]
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String completedModules = "[]";

    @Column
    @Builder.Default
    private String currentModule = "aptitude";

    @Column
    @Builder.Default
    private int overallProgress = 0;

    @Column
    @Builder.Default
    private int testsTaken = 0;

    @Column
    @Builder.Default
    private int averageScore = 0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
