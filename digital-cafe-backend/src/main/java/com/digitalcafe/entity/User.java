package com.digitalcafe.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**

 * Core User entity representing all system users across different roles.
 * Supports approval workflow + email verification + password setup.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================= BASIC INFO =================

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "government_id_type")
    private String governmentIdType;

    @Column(name = "government_id_number", unique = true)
    private String governmentIdNumber;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**

     * Password is nullable because user sets it after approval.
     */
    @Column(name = "password")
    private String password;

    // ================= STATUS FLAGS =================

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_profile_complete", nullable = false)
    @Builder.Default
    private Boolean isProfileComplete = false;

    @Column(name = "profile_completion_percentage")
    @Builder.Default
    private Integer profileCompletionPercentage = 0;

    @Column(name = "is_temp_password", nullable = false)
    @Builder.Default
    private Boolean isTempPassword = false;

    @Column(name = "must_reset_password", nullable = false)
    @Builder.Default
    private Boolean mustResetPassword = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ================= APPROVAL WORKFLOW =================

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "verification_token", length = 200)
    private String verificationToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ================= ROLES =================

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ================= RELATIONSHIPS =================

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_id")
    private Cafe cafe;

    // ================= BUSINESS LOGIC =================

    /**

     * User can access system only if:
     * active + verified + profile complete + approved
     */
    public boolean canAccessSystem() {
        return isActive
                && isEmailVerified
                && isProfileComplete
                && status == UserStatus.ACTIVE;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(Role.RoleName roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }
}
