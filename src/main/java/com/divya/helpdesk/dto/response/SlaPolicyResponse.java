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
public class SlaPolicyResponse {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private Long subCategoryId;
    private String subCategoryName;
    private Integer durationMinutes;
    private Integer warningMinutes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
