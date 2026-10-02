package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDEmployeeSkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDEmployeeSkillRepository extends JpaRepository<HDEmployeeSkillEntity, Long> {

    List<HDEmployeeSkillEntity> findByEmployeeId(Long employeeId);
}