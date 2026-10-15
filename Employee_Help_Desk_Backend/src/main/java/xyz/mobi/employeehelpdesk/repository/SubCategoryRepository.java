package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.SubCategory;

import java.util.List;

public interface SubCategoryRepository
        extends JpaRepository<SubCategory, Long> {
    List<SubCategory> findByCategoryIdAndIsActiveTrue(Long categoryId);
}