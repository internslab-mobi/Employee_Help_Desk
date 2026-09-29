package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.SubCategory;

public interface SubCategoryRepository
        extends JpaRepository<SubCategory, Long> {

}