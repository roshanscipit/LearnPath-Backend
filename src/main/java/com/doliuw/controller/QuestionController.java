package com.doliuw.controller;

import com.doliuw.dto.QuestionDtos.*;
import com.doliuw.entity.User;
import com.doliuw.service.AdminService;
import com.doliuw.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final AdminService adminService;

    // ── Public: get shuffled quiz session (JWT required for users) ──

    /** POST /api/questions/session — get 5-10 shuffled questions */
    @PostMapping("/api/questions/session")
    public ResponseEntity<QuizSessionDto> getQuizSession(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody QuizSessionRequest req) {
        return ResponseEntity.ok(questionService.getQuizSession(req));
    }

    /** POST /api/questions/check — check a single answer */
    @PostMapping("/api/questions/check")
    public ResponseEntity<CheckAnswerResponse> checkAnswer(
            @AuthenticationPrincipal User user,
            @RequestBody CheckAnswerRequest req) {
        return ResponseEntity.ok(questionService.checkAnswer(req));
    }

    /** GET /api/questions/stats — counts by type (public for display) */
    @GetMapping("/api/questions/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(questionService.getQuestionStats());
    }

    // ── Admin CRUD ──────────────────────────────────────────────────

    /** GET /api/admin/questions?type=CODING */
    @GetMapping("/api/admin/questions")
    public ResponseEntity<List<QuestionDetailDto>> getQuestions(
            @AuthenticationPrincipal User admin,
            @RequestParam(required = false) String type) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(questionService.getAllQuestions(type));
    }

    /** POST /api/admin/questions */
    @PostMapping("/api/admin/questions")
    public ResponseEntity<QuestionDetailDto> createQuestion(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody CreateQuestionRequest req) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(questionService.createQuestion(req));
    }

    /** PUT /api/admin/questions/{id} */
    @PutMapping("/api/admin/questions/{id}")
    public ResponseEntity<QuestionDetailDto> updateQuestion(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody CreateQuestionRequest req) {
        adminService.requireAdmin(admin);
        return ResponseEntity.ok(questionService.updateQuestion(id, req));
    }

    /** DELETE /api/admin/questions/{id} */
    @DeleteMapping("/api/admin/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        adminService.requireAdmin(admin);
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
