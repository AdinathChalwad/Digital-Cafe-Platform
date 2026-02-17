package com.digitalcafe.controller;

import com.digitalcafe.dto.request.LoginRequest;
import com.digitalcafe.dto.request.RegisterRequest;
import com.digitalcafe.dto.request.SetPasswordRequest;
import com.digitalcafe.dto.response.AuthResponse;
import com.digitalcafe.dto.response.RegisterResponse;
import com.digitalcafe.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =====================================================
    // 1️⃣ USER REGISTRATION → SEND VERIFY EMAIL
    // =====================================================
    @PostMapping("/api/auth/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.comprehensiveRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =====================================================
    // 2️⃣ EMAIL VERIFICATION (FROM EMAIL LINK)
    // Example Link:
    // http://localhost:8080/api/auth/verify-email?token=abc123
    // =====================================================
    @GetMapping("/api/auth/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {

        authService.verifyEmail(token);

        return ResponseEntity.ok(
                Map.of("message", "Email verified successfully. Await admin approval.")
        );
    }

    // =====================================================
    // 3️⃣ ADMIN APPROVES USER (ONLY ADMIN SHOULD CALL)
    // Matches SecurityConfig: /api/admin/**
    // =====================================================
    @PostMapping("/api/auth/admin/users/approve/{userId}")
    public ResponseEntity<Map<String, String>> approveUser(@PathVariable Long userId) {

        authService.approveUser(userId);

        return ResponseEntity.ok(
                Map.of("message", "User approved. Set-password mail sent.")
        );
    }

    // =====================================================
    // 4️⃣ USER SETS PASSWORD FROM EMAIL LINK
    // Angular calls this API
    // =====================================================
    @PostMapping("/api/auth/set-password")
    public ResponseEntity<Map<String, String>> setPassword(
            @Valid @RequestBody SetPasswordRequest request) {

        authService.setPassword(request.getToken(), request.getPassword());

        return ResponseEntity.ok(
                Map.of("message", "Password set successfully. You can now login.")
        );
    }

    // =====================================================
    // 5️⃣ LOGIN (ONLY ACTIVE USERS)
    // =====================================================
    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // LOGOUT
    // =====================================================
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}