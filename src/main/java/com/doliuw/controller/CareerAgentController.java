package com.doliuw.controller;

import com.doliuw.dto.CareerAgentDtos.CareerAgentResponse;
import com.doliuw.entity.User;
import com.doliuw.service.CareerAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI Career Agent – JWT-protected (falls under anyRequest().authenticated()
 * in SecurityConfig, same as /api/progress and /api/bookings).
 *
 * POST /api/career-agent/analyze  – role + years of experience (+ optional resume) -> personalized plan
 * GET  /api/career-agent/profile  – last saved plan for the current user, if any
 */
@RestController
@RequestMapping("/api/career-agent")
@RequiredArgsConstructor
public class CareerAgentController {

    private final CareerAgentService careerAgentService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CareerAgentResponse> analyze(
            @AuthenticationPrincipal User user,
            @RequestParam("targetRole") String targetRole,
            @RequestParam("yearsOfExperience") Integer yearsOfExperience,
            @RequestParam(value = "additionalContext", required = false) String additionalContext,
            @RequestParam(value = "resume", required = false) MultipartFile resume) {

        CareerAgentResponse result = careerAgentService.analyze(
            user, targetRole, yearsOfExperience, additionalContext, resume);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    public ResponseEntity<CareerAgentResponse> getSavedRecommendation(@AuthenticationPrincipal User user) {
        CareerAgentResponse saved = careerAgentService.getSavedRecommendation(user.getId());
        if (saved == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(saved);
    }
}
