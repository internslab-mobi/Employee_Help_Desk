package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Category;

import java.util.List;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {
    List<Category> findByDepartmentIdAndIsActiveTrue(Long departmentId);
    List<Category> findByIsActiveTrue();
}