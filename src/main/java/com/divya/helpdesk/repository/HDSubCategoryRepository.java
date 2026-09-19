package com.divya.helpdesk.repository;

import com.divya.helpdesk.entity.HDSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HDSubCategoryRepository extends JpaRepository<HDSubCategory, Long> {
    List<HDSubCategory> findByCategoryId(Long categoryId);
    List<HDSubCategory> findByCategoryIdAndIsActiveTrue(Long categoryId);
    boolean existsByCategoryIdAndName(Long categoryId, String name);
}
