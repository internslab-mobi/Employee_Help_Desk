package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDRefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HDRefreshTokenRepository extends JpaRepository<HDRefreshTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HDRefreshTokenEntity r WHERE r.token = :token")
    Optional<HDRefreshTokenEntity> findByTokenForUpdate(@Param("token") String token);
}
