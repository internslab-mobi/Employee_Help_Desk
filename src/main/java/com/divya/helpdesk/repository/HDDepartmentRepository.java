package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDDepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HDDepartmentRepository extends JpaRepository<HDDepartmentEntity, Long> {
}
