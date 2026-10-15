package xyz.mobi.employeehelpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.employeehelpdesk.entity.ErrorCodeMapping;

import java.util.Optional;

public interface ErrorCodeMappingRepository extends JpaRepository<ErrorCodeMapping, Long> {

    Optional<ErrorCodeMapping> findByExceptionType(String exceptionType);
}
