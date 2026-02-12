package com.digitalcafe.service;

import com.digitalcafe.dto.request.LoginRequest;
import com.digitalcafe.dto.request.SimpleRegisterRequest;
import com.digitalcafe.dto.request.RegisterRequest;
import com.digitalcafe.dto.request.ResetPasswordRequest;
import com.digitalcafe.dto.response.AuthResponse;
import com.digitalcafe.dto.response.RegisterResponse;

public interface AuthService {

    // ================= REGISTER =================

    AuthResponse register(SimpleRegisterRequest request);

    RegisterResponse comprehensiveRegister(RegisterRequest request);


    // ================= LOGIN =================

    AuthResponse login(LoginRequest request);


    // ================= EMAIL VERIFICATION =================

    void verifyEmail(String token);

    void resendVerificationEmail(String email);


    // ================= PASSWORD =================

    // after admin approval
    void setPassword(String token, String password);

    // forgot password flow
    void forgotPassword(String email);

    void resetPassword(String token, ResetPasswordRequest request);

    void changePassword(String username, String oldPassword, String newPassword);


    // ================= TOKEN =================

    AuthResponse refreshToken(String refreshToken);
}
