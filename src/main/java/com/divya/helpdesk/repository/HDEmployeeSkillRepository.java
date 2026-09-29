package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDEmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDEmployeeSkillRepository extends JpaRepository<HDEmployeeSkill, Long> {

    List<HDEmployeeSkill> findByEmployee_Id(Long employeeId);

}