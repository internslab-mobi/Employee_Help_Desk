package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.SlaPolicy;

import java.util.Optional;

public interface SlaPolicyRepository
        extends JpaRepository<SlaPolicy, Long> {

    @EntityGraph(attributePaths = {"department", "subCategory"})
    Optional<SlaPolicy> findById(Long id);

    @EntityGraph(attributePaths = {"department", "subCategory"})
    Optional<SlaPolicy> findByDepartmentIdAndSubCategoryIdAndIsActiveTrue(
            Long departmentId,
            Long subCategoryId
    );
}