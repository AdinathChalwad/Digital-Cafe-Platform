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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =====================================================
    // USER REGISTRATION (GOES TO ADMIN APPROVAL)
    // =====================================================
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.comprehensiveRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =====================================================
    // ADMIN APPROVES USER
    // =====================================================
    @PostMapping("/admin/approve/{userId}")
    public ResponseEntity<Map<String, String>> approveUser(@PathVariable Long userId) {
        authService.approveUser(userId);
        return ResponseEntity.ok(Map.of("message", "User approved and password setup mail sent"));
    }

    // =====================================================
    // USER SETS PASSWORD FROM EMAIL LINK
    // =====================================================
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, String>> setPassword(
            @RequestBody SetPasswordRequest request) {

        authService.setPassword(request.getToken(), request.getPassword());

        return ResponseEntity.ok(
                Map.of("message", "Password set successfully. You can now login.")
        );
    }


    // =====================================================
    // LOGIN (ONLY AFTER ACTIVE)
    // =====================================================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // LOGOUT
    // =====================================================
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
