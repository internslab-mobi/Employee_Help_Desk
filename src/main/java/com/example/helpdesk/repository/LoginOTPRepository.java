package com.example.helpdesk.repository;

import com.example.helpdesk.entity.Employee;
import com.example.helpdesk.entity.LoginOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LoginOTPRepository extends JpaRepository<LoginOTP, Long> {

    Optional<LoginOTP> findByEmployeeIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(Long employeeId, Instant now);

    Optional<LoginOTP> findFirstByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}



