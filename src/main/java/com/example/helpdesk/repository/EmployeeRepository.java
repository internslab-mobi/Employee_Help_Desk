package com.example.helpdesk.repository;

import com.example.helpdesk.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
<<<<<<< HEAD
=======

    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    List<Employee> findByDepartmentId(Long departmentId);
    List<Employee> findByRole(String role);
>>>>>>> c5b2367e (updated reworks)
}



