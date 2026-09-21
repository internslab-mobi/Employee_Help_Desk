package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.SubCategory;

import java.util.List;

public interface SubCategoryRepository
        extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByCategoryId(
            Long categoryId
    );

    List<SubCategory> findByCategoryIdAndIsActiveTrue(
            Long categoryId
    );

    List<SubCategory> findByIsActiveTrue();

    boolean existsByCategoryIdAndName(
            Long categoryId,
            String name
    );

    boolean existsByCategoryIdAndNameAndIdNot(
            Long categoryId,
            String name,
            Long id
    );
}