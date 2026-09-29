package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HDErrorCodeRepository extends JpaRepository<HDErrorCode, Long> {
}
