package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = {"employee", "employee.department"})
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") Long employeeId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
        SET r.usageCount = r.usageCount + 1,
            r.revoked = CASE
                WHEN (r.usageCount + 1) >= r.maxUses THEN true
                ELSE r.revoked
            END
        WHERE r.token = :token
          AND r.revoked = false
          AND r.usageCount < r.maxUses
          AND r.expiresAt > :now
    """)
    Integer incrementUsageIfValid(
            @Param("token") String token,
            @Param("now") Instant now
    );

}
