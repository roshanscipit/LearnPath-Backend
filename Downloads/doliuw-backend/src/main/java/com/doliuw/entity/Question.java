package com.doliuw.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores all practice questions (coding, aptitude, system design).
 * Questions are served shuffled, 5-10 at a time per session.
 */
@Entity
@Table(name = "questions", indexes = {
    @Index(name = "idx_question_type", columnList = "questionType"),
    @Index(name = "idx_question_difficulty", columnList = "difficulty"),
    @Index(name = "idx_question_company", columnList = "companyTag")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CODING | APTITUDE | SYSTEM_DESIGN */
    @Column(nullable = false)
    private String questionType;

    /** The question text / problem statement */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** For MCQ: comma-separated or JSON options */
    @Column(columnDefinition = "TEXT")
    private String options;

    /** Correct answer or model answer */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** Explanation / hints */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /** Easy | Medium | Hard */
    @Column(nullable = false)
    private String difficulty;

    /** Topic/tag: Arrays, DP, LLD, HLD, Quantitative, etc. */
    @Column
    private String topic;

    /** Optional company tag: Google, Amazon, Flipkart, etc. */
    @Column
    private String companyTag;

    /** For CODING: language hint e.g. Java, Python, Any */
    @Column
    private String language;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
