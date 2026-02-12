package com.digitalcafe.service.impl;

import com.digitalcafe.dto.request.*;
import com.digitalcafe.dto.response.AuthResponse;
import com.digitalcafe.dto.response.RegisterResponse;
import com.digitalcafe.entity.*;
import com.digitalcafe.exception.BadRequestException;
import com.digitalcafe.exception.ResourceNotFoundException;
import com.digitalcafe.repository.*;
import com.digitalcafe.security.JwtUtil;
import com.digitalcafe.service.AuthService;
import com.digitalcafe.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;


    // =====================================================
    // REGISTER
    // =====================================================

    @Override
    @Transactional
    public AuthResponse register(SimpleRegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already registered");

        Role role = roleRepository.findByName(Role.RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role","name","CUSTOMER"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setPassword(null);
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setIsProfileComplete(false);
        user.setMustResetPassword(false);
        user.setStatus(UserStatus.PENDING);

        user.getRoles().add(role);
        userRepository.save(user);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), token.getToken(), null);

        return AuthResponse.builder()
                .email(user.getEmail())
                .status(user.getStatus().name())
                .message("Registration successful. Please verify email.")
                .build();
    }

    // =====================================================
    // Full Profile REGISTER
    // =====================================================

    @Override
    @Transactional
    public RegisterResponse comprehensiveRegister(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername()))
            throw new BadRequestException("Username already exists");

        Role.RoleName roleName = Role.RoleName.valueOf(request.getRole());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role","name",request.getRole()));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getPersonalDetails().getEmail());
        user.setPassword(null);
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setIsProfileComplete(false);
        user.setMustResetPassword(false);
        user.setStatus(UserStatus.PENDING);

        user.getRoles().add(role);
        user = userRepository.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(request.getPersonalDetails().getFirstName());
        profile.setLastName(request.getPersonalDetails().getLastName());
        profile.setPhoneNumber(request.getPersonalDetails().getPhone());
        profile.setGender(Profile.Gender.valueOf(request.getPersonalDetails().getGender()));

        user.setProfile(profile);
        userRepository.save(user);

        return RegisterResponse.builder()
                .message("Registration submitted. Await admin approval.")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(request.getRole())
                .emailVerified(false)
                .profileCompleted(false)
                .profileCompletionPercentage(0)
                .build();
    }

    // =====================================================
    // VERIFY EMAIL
    // =====================================================

    @Override
    @Transactional
    public void verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByToken(token)
                        .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Verification token expired");

        User user = verificationToken.getUser();
        user.setIsEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.delete(verificationToken);

        log.info("Email verified for {}", user.getEmail());
    }


    // =====================================================
    // RESEND VERIFICATION EMAIL
    // =====================================================

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User","email",email));

        if (user.getIsEmailVerified())
            throw new BadRequestException("Email already verified");

        emailVerificationTokenRepository.deleteByUser(user);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), token.getToken(), null);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!user.getIsEmailVerified())
            throw new BadRequestException("Please verify your email first");

        if (user.getStatus() != UserStatus.ACTIVE)
            throw new BadRequestException("Account not activated yet");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtil.generateToken(authentication);
        String refreshToken = jwtUtil.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).toList())
                .status(user.getStatus().name())
                .message("Login successful")
                .build();
    }


    // =====================================================
    // SET PASSWORD AFTER APPROVAL
    // =====================================================

    @Override
    @Transactional
    public void setPassword(String token, String password) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByToken(token)
                        .orElseThrow(() -> new BadRequestException("Invalid token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Token expired");

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
    }


    // =====================================================
    // FORGOT PASSWORD
    // =====================================================

    @Override
    @Transactional
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User","email",email));

        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
    }


    // =====================================================
    // RESET PASSWORD
    // =====================================================

    @Override
    @Transactional
    public void resetPassword(String token, ResetPasswordRequest request) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByToken(token)
                        .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Reset token expired");

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
    }


    // =====================================================
    // CHANGE PASSWORD
    // =====================================================

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User","email",username));

        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new BadRequestException("Old password incorrect");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    // =====================================================
    // REFRESH TOKEN
    // =====================================================

    @Override
    public AuthResponse refreshToken(String refreshToken) {

        if (!jwtUtil.validateToken(refreshToken))
            throw new BadRequestException("Invalid refresh token");

        String username = jwtUtil.extractUsername(refreshToken);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User","email",username));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + r.getName().name()))
                        .toList()
        );

        String newAccessToken = jwtUtil.generateToken(authentication);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .message("Token refreshed")
                .build();
    }
}
