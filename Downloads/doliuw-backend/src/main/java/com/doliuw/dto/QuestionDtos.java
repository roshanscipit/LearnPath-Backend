package com.doliuw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class QuestionDtos {

    /** Returned to frontend — no internal DB details exposed */
    @Data
    public static class QuestionDto {
        private Long id;
        private String questionType;   // CODING | APTITUDE | SYSTEM_DESIGN
        private String questionText;
        private List<String> options;  // null for open-ended
        private String difficulty;
        private String topic;
        private String companyTag;
        private String language;
    }

    /** Returned to admin with full answer + explanation */
    @Data
    public static class QuestionDetailDto extends QuestionDto {
        private String answer;
        private String explanation;
        private boolean active;
        private LocalDateTime createdAt;
    }

    /** Request to get a shuffled quiz session */
    @Data
    public static class QuizSessionRequest {
        @NotBlank
        private String questionType;   // CODING | APTITUDE | SYSTEM_DESIGN | MIXED
        private String difficulty;     // optional filter
        private String companyTag;     // optional
        private int count = 10;        // 5–10, clamped server-side
    }

    /** A quiz session: shuffled questions with no answers (answers checked separately) */
    @Data
    public static class QuizSessionDto {
        private String sessionId;
        private String questionType;
        private String difficulty;
        private List<QuestionDto> questions;
        private int totalQuestions;
    }

    /** Admin: create/update a question */
    @Data
    public static class CreateQuestionRequest {
        @NotBlank private String questionType;
        @NotBlank private String questionText;
        private String options;        // JSON or comma-separated
        private String answer;
        private String explanation;
        @NotBlank private String difficulty;
        private String topic;
        private String companyTag;
        private String language;
        private boolean active = true;
    }

    /** Check answer for a question */
    @Data
    public static class CheckAnswerRequest {
        @NotNull  private Long questionId;
        @NotBlank private String userAnswer;
    }

    @Data
    public static class CheckAnswerResponse {
        private Long questionId;
        private boolean correct;
        private String correctAnswer;
        private String explanation;
    }
}
