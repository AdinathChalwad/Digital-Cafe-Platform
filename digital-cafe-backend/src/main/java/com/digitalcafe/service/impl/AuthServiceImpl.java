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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // =====================================================
    // REGISTER (STEP-1 REGISTRATION → PENDING APPROVAL)
    // =====================================================

    @Override
    @Transactional
    public RegisterResponse comprehensiveRegister(RegisterRequest request) {

        String email = request.getPersonalDetails().getEmail();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }

        Role.RoleName roleName = Role.RoleName.valueOf(request.getRole());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        // ======================
        // CREATE USER (NO PASSWORD YET)
        // ======================
        User user = new User();
        user.setEmail(email);
        user.setUsername(email); // email is login username
        user.setPassword(null);  // password will be set later
        user.setIsActive(false);
        user.setIsEmailVerified(true);
        user.setIsProfileComplete(false);
        user.setMustResetPassword(true);
        user.setStatus(UserStatus.PENDING);

        user.getRoles().add(role);
        user = userRepository.save(user);

        // ======================
        // CREATE PROFILE
        // ======================
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName(request.getPersonalDetails().getFirstName());
        profile.setLastName(request.getPersonalDetails().getLastName());
        profile.setPhoneNumber(request.getPersonalDetails().getPhone());

        if (request.getPersonalDetails().getGender() != null) {
            profile.setGender(Profile.Gender.valueOf(request.getPersonalDetails().getGender()));
        }

        user.setProfile(profile);
        userRepository.save(user);

        log.info("User registered and waiting for admin approval: {}", email);

        return RegisterResponse.builder()
                .message("Registration submitted. Await admin approval.")
                .userId(user.getId())
                .username(user.getEmail())
                .email(user.getEmail())
                .role(request.getRole())
                .emailVerified(true)
                .profileCompleted(false)
                .profileCompletionPercentage(50)
                .build();
    }


    // =====================================================
    // ADMIN APPROVES USER → SEND SET PASSWORD MAIL
    // =====================================================

    @Transactional
    public void approveUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        passwordResetTokenRepository.save(token);

        emailService.sendSetPasswordMail(user.getEmail(), token.getToken());

        log.info("Approval mail sent to {}", user.getEmail());
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
        user.setIsActive(true);

        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password set successfully for {}", user.getEmail());
    }


    // =====================================================
    // LOGIN (ONLY AFTER ACTIVE)
    // =====================================================

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

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
}
