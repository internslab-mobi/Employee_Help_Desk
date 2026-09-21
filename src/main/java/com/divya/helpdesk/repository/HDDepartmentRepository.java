package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDDepartmentRepository extends JpaRepository<HDDepartment, Long> {
    boolean existsByCode(String code);
}
