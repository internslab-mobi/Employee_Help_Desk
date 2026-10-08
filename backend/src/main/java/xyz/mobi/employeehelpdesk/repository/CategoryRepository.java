package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.Category;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

}