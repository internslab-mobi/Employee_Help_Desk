package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.SlaPolicy;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepository
        extends JpaRepository<SlaPolicy, Long> {

    List<SlaPolicy> findByDepartmentId(Long departmentId);

    List<SlaPolicy> findByDepartmentIdAndIsActiveTrue(Long departmentId);

    List<SlaPolicy> findBySubCategoryId(Long subCategoryId);

    List<SlaPolicy> findBySubCategoryIdAndIsActiveTrue(Long subCategoryId);

    List<SlaPolicy> findByDepartmentIdAndSubCategoryId(Long departmentId, Long subCategoryId);

    List<SlaPolicy> findByIsActiveTrue();

    boolean existsByDepartmentIdAndSubCategoryId(Long departmentId, Long subCategoryId);

    boolean existsByDepartmentIdAndSubCategoryIdAndIdNot(Long departmentId, Long subCategoryId, Long id);

    Optional<SlaPolicy> findByDepartmentIdAndSubCategoryIdAndIsActiveTrue(
            Long departmentId,
            Long subCategoryId
    );
}