package xyz.mobi.employeehelpdesk.dto.employee;

import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record EmployeeCreateResponseDTO(
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
        Instant createdAt
) {
}
