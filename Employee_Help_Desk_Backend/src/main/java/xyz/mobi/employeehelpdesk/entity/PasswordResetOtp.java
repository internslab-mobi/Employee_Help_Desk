package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "hd_password_reset_otps", indexes = {
        @Index(name = "idx_otp_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetOtp extends BaseEntity {

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant otpExpiresAt;

    @Builder.Default
    @Column(name = "otp_attempt_count", nullable = false)
    private int otpAttemptCount = 0;

    @Builder.Default
    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified = false;

    @Column(name = "reset_token_hash", length = 255)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

    @Builder.Default
    @Column(name = "reset_token_used", nullable = false)
    private boolean resetTokenUsed = false;
}
