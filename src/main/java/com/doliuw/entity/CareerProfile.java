package com.doliuw.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores the inputs and last AI-generated recommendation for the
 * "AI Career Agent" feature: target role, years of experience,
 * parsed resume text, and the last recommendation payload (JSON).
 */
@Entity
@Table(name = "career_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CareerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private String targetRole;

    @Column
    private Integer yearsOfExperience;

    @Column
    private String resumeFileName;

    // Extracted plain text from the uploaded PDF/DOCX resume
    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String additionalContext;

    // Last AI recommendation response, stored as JSON so the page can
    // reload it without re-calling the AI on every visit.
    @Column(columnDefinition = "TEXT")
    private String lastRecommendationJson;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
