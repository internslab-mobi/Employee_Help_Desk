package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface HDRefreshTokenRepository extends JpaRepository<HDRefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HDRefreshToken r WHERE r.token = :token")
    Optional<HDRefreshToken> findByTokenForUpdate(@Param("token") String token);
}
