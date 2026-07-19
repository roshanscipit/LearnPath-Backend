package com.doliuw.controller;

import com.doliuw.dto.AppDtos.*;
import com.doliuw.entity.User;
import com.doliuw.service.UserProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class UserProgressController {

    private final UserProgressService progressService;

    // GET /api/progress  – returns current user's progress
    @GetMapping
    public ResponseEntity<UserProgressDto> getProgress(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(progressService.getProgress(user.getId()));
    }

    // PUT /api/progress  – update progress (called after completing a module / taking a test)
    @PutMapping
    public ResponseEntity<UserProgressDto> updateProgress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProgressRequest req) {
        return ResponseEntity.ok(progressService.updateProgress(user.getId(), req));
    }
}
