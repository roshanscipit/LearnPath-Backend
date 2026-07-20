package com.doliuw.service;

import com.doliuw.dto.AuthDtos.*;
import com.doliuw.entity.OtpStore;
import com.doliuw.entity.User;
import com.doliuw.entity.UserProgress;
import com.doliuw.exception.AppException;
import com.doliuw.repository.OtpRepository;
import com.doliuw.repository.UserProgressRepository;
import com.doliuw.repository.UserRepository;
import com.doliuw.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserProgressRepository progressRepository;
    private final OtpRepository otpRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.demo-mode}")
    private boolean demoMode;

    // ─── Email Signup ─────────────────────────────────────────────

    @Transactional
    public AuthResponse signupEmail(EmailSignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .provider(User.AuthProvider.EMAIL)
            .avatar(generateAvatarUrl(req.getName()))
            .build();

        user = userRepository.save(user);
        createDefaultProgress(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, toDto(user));
    }

    // ─── Email Login ──────────────────────────────────────────────

    @Cacheable(value = "users", key = "#email")
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public AuthResponse loginEmail(EmailLoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, toDto(user));
    }

    // ─── OTP Flow ─────────────────────────────────────────────────

    @Transactional
    public String sendOtp(SendOtpRequest req) {
        String mobile = req.getMobile();

        // Invalidate old OTPs for this number
        otpRepository.invalidatePreviousOtps(mobile);

        String otp = demoMode ? "123456" : generateOtp();

        OtpStore otpStore = OtpStore.builder()
            .mobile(mobile)
            .otp(otp)
            .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
            .build();

        otpRepository.save(otpStore);

        if (demoMode) {
            log.info("[DEMO] OTP for {}: {}", mobile, otp);
        } else {
            // TODO: integrate Twilio / MSG91 here
            // twilioService.sendSms("+91" + mobile, "Your Doliuw OTP: " + otp);
            log.info("OTP sent to {}", mobile);
        }

        return otp; // returned only for demo mode logging; controller won't expose it
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        OtpStore otpStore = otpRepository
            .findTopByMobileAndUsedFalseOrderByCreatedAtDesc(req.getMobile())
            .orElseThrow(() -> new AppException("No OTP found. Please request a new one.", HttpStatus.BAD_REQUEST));

        if (LocalDateTime.now().isAfter(otpStore.getExpiryTime())) {
            throw new AppException("OTP expired. Please request a new one.", HttpStatus.BAD_REQUEST);
        }

        if (!otpStore.getOtp().equals(req.getOtp())) {
            throw new AppException("Invalid OTP.", HttpStatus.UNAUTHORIZED);
        }

        otpStore.setUsed(true);
        otpRepository.save(otpStore);

        // Find or create user
        User user = userRepository.findByMobile(req.getMobile()).orElseGet(() -> {
            String name = (req.getName() != null && !req.getName().isBlank())
                ? req.getName()
                : "User" + req.getMobile().substring(req.getMobile().length() - 4);

            User newUser = User.builder()
                .name(name)
                .mobile(req.getMobile())
                .provider(User.AuthProvider.MOBILE)
                .avatar(generateAvatarUrl(name))
                .build();
            return userRepository.save(newUser);
        });

        // Create progress if missing
        if (progressRepository.findByUserId(user.getId()).isEmpty()) {
            createDefaultProgress(user);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getMobile());
        return new AuthResponse(token, toDto(user));
    }

    // ─── Google OAuth ─────────────────────────────────────────────

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Transactional
    public AuthResponse googleSignIn(String idTokenString) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new AppException(
                "Google sign-in is not configured on the server (missing GOOGLE_CLIENT_ID).",
                HttpStatus.NOT_IMPLEMENTED
            );
        }

        com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
            new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance())
                .setAudience(java.util.Collections.singletonList(googleClientId))
                .build();

        com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            throw new AppException("Failed to verify Google token.", HttpStatus.UNAUTHORIZED);
        }

        if (idToken == null) {
            throw new AppException("Invalid or expired Google token.", HttpStatus.UNAUTHORIZED);
        }

        com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        // 1. Try matching by googleId first
        User user = userRepository.findByGoogleId(googleId).orElse(null);

        // 2. Fall back to matching an existing account by email (link accounts)
        if (user == null && email != null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setGoogleId(googleId);
                if (user.getProvider() == User.AuthProvider.EMAIL) {
                    // keep provider as-is; googleId link is enough to allow Google login too
                }
            }
        }

        // 3. No existing user — create a new one
        if (user == null) {
            user = User.builder()
                .name(name != null ? name : "Google User")
                .email(email)
                .googleId(googleId)
                .provider(User.AuthProvider.GOOGLE)
                .avatar(picture != null ? picture : generateAvatarUrl(name != null ? name : "User"))
                .build();
            user = userRepository.save(user);
            createDefaultProgress(user);
        } else {
            user = userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, toDto(user));
    }

    // ─── Get Current User ─────────────────────────────────────────

    @Cacheable(value = "users", key = "#userId")
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return toDto(user);
    }

    // ─── Logout (stateless – client drops token; we just return 200) ──

    public void logout(Long userId) {
        // JWT is stateless. For token blacklisting, integrate Redis here.
        log.info("User {} logged out", userId);
    }

    // ─── Helpers ──────────────────────────────────────────────────

    @CacheEvict(value = "users", key = "#user.id")
    public void evictUserCache(User user) { }

    private void createDefaultProgress(User user) {
        UserProgress progress = UserProgress.builder()
            .user(user)
            .selectedRole("java")
            .selectedVariant("Full Stack")
            .completedModules("[]")
            .currentModule("aptitude")
            .overallProgress(0)
            .build();
        progressRepository.save(progress);
    }

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setMobile(user.getMobile());
        dto.setAvatar(user.getAvatar());
        dto.setProvider(user.getProvider().name());
        return dto;
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(999999));
    }

    private String generateAvatarUrl(String name) {
        return "https://ui-avatars.com/api/?name=" +
               name.replace(" ", "+") +
               "&background=000000&color=ffffff&bold=true";
    }
}
