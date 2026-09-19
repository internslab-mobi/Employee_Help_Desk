package com.divya.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentManagerResponse {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String email;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}
