package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HDDepartmentRepository extends JpaRepository<HDDepartment, Long> {
    Optional<HDDepartment> findByCode(String code);
    boolean existsByCode(String code);
    List<HDDepartment> findByIsActiveTrue();
}
