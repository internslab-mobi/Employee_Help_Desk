package com.example.helpdesk.dto.response;

import com.example.helpdesk.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String designation;
    private DepartmentResponseDTO department;
    private String employmentStatus;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;
    private Role role;
    private String timezone;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentResponseDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Boolean active;
    }
}
