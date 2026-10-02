package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDErrorCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HDErrorCodeRepository extends JpaRepository<HDErrorCodeEntity, Long> {
}
