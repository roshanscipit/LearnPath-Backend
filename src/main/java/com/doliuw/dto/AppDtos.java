package com.doliuw.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public class AppDtos {

    // ─── Progress ────────────────────────────────────────────────

    @Data
    public static class UserProgressDto {
        private String selectedRole;
        private String selectedVariant;
        private List<String> completedModules;
        private String currentModule;
        private int overallProgress;
        private int testsTaken;
        private int averageScore;
    }

    @Data
    public static class UpdateProgressRequest {
        @NotBlank private String selectedRole;
        @NotBlank private String selectedVariant;
        private List<String> completedModules;
        @NotBlank private String currentModule;
        private int overallProgress;
        private int testsTaken;
        private int averageScore;
    }

    // ─── Booking ─────────────────────────────────────────────────

    @Data
    public static class BookingRequest {
        @NotBlank private String serviceId;
        @NotBlank private String serviceName;
        @Min(0)           private int price;
        @NotNull  private LocalDate bookingDate;
        @NotBlank private String timeSlot;
    }

    @Data
    public static class BookingDto {
        private Long id;
        private String serviceId;
        private String serviceName;
        private int price;
        private LocalDate bookingDate;
        private String timeSlot;
        private String status;
        private String createdAt;
    }

    // ─── Generic ─────────────────────────────────────────────────

    @Data
    public static class MessageResponse {
        private String message;
        public MessageResponse(String message) { this.message = message; }
    }
}
