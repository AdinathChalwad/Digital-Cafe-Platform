package com.digitalcafe.service.impl;

import com.digitalcafe.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

// =====================================================
// CORE EMAIL SENDER
// =====================================================

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }

// =====================================================
// APPROVAL FLOW EMAIL
// =====================================================

    @Override
    public void sendSetPasswordMail(String to, String token) {

        String link = "http://localhost:4200/auth/set-password?token=" + token;

        String subject = "Account Approved — Set Your Password";

        String body =
                "Your account has been approved!\n\n"
                        + "Click the link below to set your password:\n"
                        + link
                        + "\n\nThis link expires in 24 hours.";

        sendEmail(to, subject, body);
    }

// =====================================================
// AUTH EMAILS
// =====================================================

    @Override
    public void sendPasswordResetEmail(String to, String token) {

        String link = "http://localhost:4200/reset-password?token=" + token;

        sendEmail(
                to,
                "Password Reset Request",
                "Click the link to reset your password:\n" + link
        );
    }

    @Override
    public void sendPasswordChangedNotification(String to) {

        sendEmail(
                to,
                "Password Changed Successfully",
                "Your password has been changed successfully.\n\n"
                        + "If this wasn't you, contact support immediately."
        );
    }

// =====================================================
// ACCOUNT EMAILS
// =====================================================

    @Override
    public void sendVerificationEmail(String to, String token, String tempPassword) {

        sendEmail(
                to,
                "Verify Your Email",
                "Your temporary password: " + tempPassword
                        + "\nVerification token: " + token
        );
    }

    @Override
    public void sendWelcomeEmail(String to, String username, String tempPassword) {

        sendEmail(
                to,
                "Welcome to Digital Cafe",
                "Hello " + username
                        + "\nYour temporary password is: "
                        + tempPassword
        );
    }

    @Override
    public void sendRegistrationSuccessEmail(String to, String username) {

        sendEmail(
                to,
                "Registration Submitted",
                "Hello " + username
                        + ",\n\nYour registration has been submitted successfully."
                        + "\nPlease wait for admin approval."
        );
    }

// =====================================================
// BUSINESS EMAILS
// =====================================================

    @Override
    public void sendOrderConfirmation(String to, String orderDetails) {

        sendEmail(
                to,
                "Order Confirmation",
                "Your order has been placed successfully!\n\n"
                        + orderDetails
        );
    }

    @Override
    public void sendBookingConfirmation(String to, String bookingDetails) {

        sendEmail(
                to,
                "Booking Confirmed",
                "Your booking is confirmed.\n\n"
                        + bookingDetails
        );
    }
    private void sendMail(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        msg.setFrom("adinathchalwad40@gmail.com");
        mailSender.send(msg);
    }

}
