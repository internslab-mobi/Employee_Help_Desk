package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.PasswordResetOtp;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE PasswordResetOtp o
        SET o.used = true
        WHERE o.id = :id
          AND o.used = false
          AND o.expiresAt > :now
    """)
    int consumeOtpIfValid(
            @Param("id") Long id,
            @Param("now") Instant now
    );

    @Modifying
    @Query("DELETE FROM PasswordResetOtp p WHERE p.email = :email")
    void deleteByEmail(@Param("email") String email);
}
