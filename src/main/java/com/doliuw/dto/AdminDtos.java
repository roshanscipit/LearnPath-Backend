package com.doliuw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminDtos {

    // ── Dashboard stats ───────────────────────────────────────────

    @Data
    public static class DashboardStats {
        private long totalUsers;
        private long totalBookings;
        private long totalRevenue;
        private long openComplaints;
        private long totalQuestions;
        private long totalCourses;
        private long totalMockCompanies;
        private long totalCodingQuestions;
        private long totalAptitudeQuestions;
        private long totalSystemDesignQuestions;
    }

    // ── User ─────────────────────────────────────────────────────

    @Data
    public static class AdminUserDto {
        private Long id;
        private String name;
        private String email;
        private String mobile;
        private String provider;
        private boolean enabled;
        private LocalDateTime createdAt;
    }

    // ── Course ───────────────────────────────────────────────────

    @Data
    public static class CourseDto {
        private Long id;
        private String title;
        private String description;
        private String category;
        private int price;
        private boolean active;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CreateCourseRequest {
        @NotBlank private String title;
        @NotBlank private String description;
        @NotBlank private String category;
        @NotNull  private Integer price;
        private boolean active = true;
    }

    // ── Mock Company ──────────────────────────────────────────────

    @Data
    public static class MockCompanyDto {
        private Long id;
        private String name;
        private String category;
        private String logoUrl;
        private String difficulty;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CreateMockCompanyRequest {
        @NotBlank private String name;
        @NotBlank private String category;
        private String logoUrl;
        @NotBlank private String difficulty;
    }

    // ── Booking (admin view) ──────────────────────────────────────

    @Data
    public static class AdminBookingDto {
        private Long id;
        private String userName;
        private String userEmail;
        private String serviceName;
        private int price;
        private LocalDate bookingDate;
        private String timeSlot;
        private String status;
        private LocalDateTime createdAt;
    }

    // ── Complaint / Help ──────────────────────────────────────────

    @Data
    public static class ComplaintDto {
        private Long id;
        private String userName;
        private String userEmail;
        private String subject;
        private String message;
        private String status; // OPEN, IN_PROGRESS, RESOLVED
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class CreateComplaintRequest {
        @NotBlank private String subject;
        @NotBlank private String message;
    }

    @Data
    public static class UpdateComplaintStatusRequest {
        @NotBlank private String status;
    }
}
