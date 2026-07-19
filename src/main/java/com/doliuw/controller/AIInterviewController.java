package com.doliuw.controller;

import com.doliuw.dto.AppDtos.*;
import com.doliuw.entity.User;
import com.doliuw.service.AIInterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Interview endpoints – all protected (JWT required).
 *
 * POST /api/ai-interview/start        – pick role/variant/difficulty, get first question
 * POST /api/ai-interview/answer       – submit an answer, get evaluation + next question
 * POST /api/ai-interview/finish       – end session, receive full scorecard
 * GET  /api/ai-interview/history      – list past sessions for the current user
 * GET  /api/ai-interview/roles        – roles available for mock interviews + online resources
 */
@RestController
@RequestMapping("/api/ai-interview")
@RequiredArgsConstructor
public class AIInterviewController {

    private final AIInterviewService aiInterviewService;

    /** Start a new mock interview session */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String roleId    = body.getOrDefault("roleId", "java");
        String variant   = body.getOrDefault("variant", "Full Stack");
        String difficulty = body.getOrDefault("difficulty", "Medium");

        return ResponseEntity.ok(aiInterviewService.startSession(user, roleId, variant, difficulty));
    }

    /** Submit an answer and get evaluation + next question */
    @PostMapping("/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");
        String questionId = body.get("questionId");
        String answer    = body.get("answer");

        return ResponseEntity.ok(aiInterviewService.evaluateAnswer(sessionId, questionId, answer));
    }

    /** Finish interview and get full report */
    @PostMapping("/finish")
    public ResponseEntity<Map<String, Object>> finishSession(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String sessionId = body.get("sessionId");
        return ResponseEntity.ok(aiInterviewService.finishSession(sessionId, user));
    }

    /** List this user's past interview sessions */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiInterviewService.getUserHistory(user));
    }

    /** Return roles available for AI interviews + their online resources */
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getInterviewRoles() {
        return ResponseEntity.ok(aiInterviewService.getInterviewRoles());
    }
}
