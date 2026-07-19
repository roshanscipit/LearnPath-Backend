package com.doliuw.controller;

import com.doliuw.dto.AppDtos.MessageResponse;
import com.doliuw.dto.AuthDtos.*;
import com.doliuw.entity.User;
import com.doliuw.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/signup/email
    @PostMapping("/signup/email")
    public ResponseEntity<AuthResponse> signupEmail(@Valid @RequestBody EmailSignupRequest req) {
        return ResponseEntity.ok(authService.signupEmail(req));
    }

    // POST /api/auth/login/email
    @PostMapping("/login/email")
    public ResponseEntity<AuthResponse> loginEmail(@Valid @RequestBody EmailLoginRequest req) {
        return ResponseEntity.ok(authService.loginEmail(req));
    }

    // POST /api/auth/send-otp
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        authService.sendOtp(req);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    // POST /api/auth/verify-otp
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(authService.verifyOtp(req));
    }

    // POST /api/auth/google  – body: { idToken: "<Google ID token from frontend>" }
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignIn(@Valid @RequestBody GoogleSignInRequest req) {
        return ResponseEntity.ok(authService.googleSignIn(req.getIdToken()));
    }

    // GET /api/auth/me  – requires JWT
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.toDto(user));
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal User user) {
        if (user != null) authService.logout(user.getId());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
