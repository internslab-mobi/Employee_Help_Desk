package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.mobi.employeehelpdesk.entity.SubCategory;

import java.util.List;
import java.util.Optional;

public interface SubCategoryRepository
        extends JpaRepository<SubCategory, Long> {

    @EntityGraph(attributePaths = {"category", "category.department", "createdBy"})
    @Query("SELECT sc FROM SubCategory sc WHERE sc.isActive = true ORDER BY sc.name ASC")
    List<SubCategory> findAllActive();

    @EntityGraph(attributePaths = {"category", "category.department", "createdBy"})
    @Query("SELECT sc FROM SubCategory sc WHERE sc.category.id = :categoryId AND sc.isActive = true ORDER BY sc.name ASC")
    List<SubCategory> findActiveByCategoryId(@Param("categoryId") Long categoryId);

    @EntityGraph(attributePaths = {"category", "category.department", "createdBy"})
    Optional<SubCategory> findById(Long id);

    @EntityGraph(attributePaths = {"category", "category.department", "createdBy"})
    Optional<SubCategory> findByIdAndIsActiveTrue(Long id);
}