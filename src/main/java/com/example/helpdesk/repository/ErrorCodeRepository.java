package com.example.helpdesk.repository;

import com.example.helpdesk.entity.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErrorCodeRepository extends JpaRepository<ErrorCode, Long> {

    Optional<ErrorCode> findByCodeAndActiveTrue(String code);
}



