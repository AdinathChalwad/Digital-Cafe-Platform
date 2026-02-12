package com.digitalcafe.service;

public interface EmailService {

    // ================= APPROVAL FLOW =================

    /**
     * Sent after admin approves user.
     * Contains link to set password.
     */
    void sendSetPasswordMail(String to, String token);


    // ================= AUTH =================

    void sendPasswordResetEmail(String to, String token);

    void sendPasswordChangedNotification(String to);


    // ================= ACCOUNT =================

    void sendVerificationEmail(String to, String token, String tempPassword);

    void sendWelcomeEmail(String to, String username, String tempPassword);

    void sendRegistrationSuccessEmail(String to, String username);


    // ================= BUSINESS =================

    void sendOrderConfirmation(String to, String orderDetails);

    void sendBookingConfirmation(String to, String bookingDetails);
}
