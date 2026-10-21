package xyz.mobi.employeehelpdesk.dto.employee;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.Instant;
import java.time.LocalDate;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeeResponseDTO(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        String designation,
        Long departmentId,
        String departmentName,
        EmploymentStatus employmentStatus,
        UserRole role,
        LocalDate dateOfJoining,
        LocalDate dateOfExit,
        String timezone,
        Instant createdAt,
        Instant updatedAt
) {
}
