package xyz.mobi.Employee_Help_Desk_Auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.mobi.Employee_Help_Desk_Auth.entity.ErrorCodeMapping;

import java.util.Optional;

public interface ErrorCodeMappingRepository extends JpaRepository<ErrorCodeMapping, Long> {

    Optional<ErrorCodeMapping> findByExceptionType(String exceptionType);
}
