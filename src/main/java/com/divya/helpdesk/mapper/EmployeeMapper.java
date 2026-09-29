package com.divya.helpdesk.mapper;

import com.divya.helpdesk.dto.user.DepartmentResponse;
import com.divya.helpdesk.dto.user.EmployeeResponse;
import com.divya.helpdesk.dto.user.EmployeeUpdateResponse;
import com.divya.helpdesk.entity.HDDepartment;
import com.divya.helpdesk.entity.HDEmployee;
import lombok.Builder;

@Builder
public class EmployeeMapper {

    public static EmployeeResponse mapToResponse(HDEmployee employee) {

        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .department(toDepartmentResponse(employee.getDepartment()))
                .role(employee.getRole())
                .employmentStatus(employee.getEmploymentStatus())
                .dateOfJoining(employee.getDateOfJoining())
                .timezone(employee.getTimezone())
                .build();
    }

    public static EmployeeUpdateResponse mapToUpdateResponse(HDEmployee employee){
        return EmployeeUpdateResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .department(toDepartmentResponse(employee.getDepartment()))
                .role(employee.getRole())
                .employmentStatus(employee.getEmploymentStatus())
                .dateOfJoining(employee.getDateOfJoining())
                .timezone(employee.getTimezone())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    public static DepartmentResponse toDepartmentResponse(HDDepartment department) {
        if (department == null) {
            return null;
        }

        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .build();
    }
}
