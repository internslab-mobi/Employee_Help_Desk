package xyz.mobi.employeehelpdesk.dto.employee;

import lombok.Builder;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record EmployeeAdminResponse(
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
        Boolean enabled,
        LocalDate dateOfJoining,
        LocalDate dateOfExit,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
