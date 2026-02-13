package com.digitalcafe.service;

import com.digitalcafe.dto.request.LoginRequest;
import com.digitalcafe.dto.request.RegisterRequest;
import com.digitalcafe.dto.response.AuthResponse;
import com.digitalcafe.dto.response.RegisterResponse;

public interface AuthService {

    // ================= REGISTRATION =================

    // Full registration submitted by user (goes to admin approval)
    RegisterResponse comprehensiveRegister(RegisterRequest request);


    // ================= ADMIN ACTION =================

    // Admin approves the user and triggers password setup email
    void approveUser(Long userId);


    // ================= PASSWORD SETUP =================

    // User sets password from email link after approval
    void setPassword(String token, String password);


    // ================= LOGIN =================

    AuthResponse login(LoginRequest request);
}
