package com.doliuw.dto;

import lombok.Data;

import java.util.List;

/**
 * Request/response DTOs for the AI Career Agent feature
 * (POST /api/career-agent/analyze, GET /api/career-agent/profile).
 */
public class CareerAgentDtos {

    @Data
    public static class RecommendedCourse {
        private String topicId;
        private String title;
        private String level;      // Basic / Intermediate / Advanced
        private String reason;     // why the AI recommends it for this user
    }

    @Data
    public static class CareerAgentResponse {
        private String targetRole;
        private String targetVariant;
        private Integer yearsOfExperience;
        private String suggestedLevel;          // Basic / Intermediate / Advanced
        private String summary;                 // 2-3 sentence overview of where they stand
        private List<String> strengths;
        private List<String> gaps;
        private List<RecommendedCourse> recommendedCourses;
        private List<String> recommendedMockTests;
        private boolean aiInterviewRecommended;
        private boolean oneOnOneRecommended;
        private String oneOnOneReason;
        private List<String> nextSteps;
        private String generatedAt;
    }
}
