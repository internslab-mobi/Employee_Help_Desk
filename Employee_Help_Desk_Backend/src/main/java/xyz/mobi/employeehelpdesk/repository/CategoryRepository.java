package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"department"})
    List<Category> findByIsActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"department"})
    List<Category> findByDepartmentIdAndIsActiveTrueOrderByNameAsc(Long departmentId);

    @EntityGraph(attributePaths = {"department"})
    Optional<Category> findById(Long id);

    @EntityGraph(attributePaths = {"department"})
    Optional<Category> findByIdAndIsActiveTrue(Long id);
}