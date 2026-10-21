package xyz.mobi.Employee_Help_Desk_Auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.Employee_Help_Desk_Auth.entity.PasswordResetOtp;
import xyz.mobi.Employee_Help_Desk_Auth.repository.PasswordResetOtpRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetOtpPersistenceService {
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PasswordResetOtp saveOtp(String email, String otp) {

        passwordResetOtpRepository.deleteByEmail(email);

        PasswordResetOtp resetOtp = PasswordResetOtp.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(otp))
                .otpExpiresAt(Instant.now().plus(2, ChronoUnit.MINUTES))
                .otpAttemptCount(0)
                .otpVerified(false)
                .resetTokenHash(null)
                .resetTokenExpiresAt(null)
                .resetTokenUsed(false)
                .build();

        return passwordResetOtpRepository.save(resetOtp);
    }

    @Transactional
    public void invalidateOtp(Long otpId) {
        passwordResetOtpRepository.deleteById(otpId);
    }
}
