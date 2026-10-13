package xyz.mobi.Employee_Help_Desk_Auth.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.Employee_Help_Desk_Auth.entity.Employee;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @EntityGraph(attributePaths = {"department"})
    Optional<Employee> findById(Long id);

    @EntityGraph(attributePaths = {"department"})
    Optional<Employee> findByEmail(String email);

    Boolean existsByEmail(String email);
}
