package com.divya.helpdesk.dto.response;

import com.divya.helpdesk.enums.HDPriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubCategoryResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long departmentId;
    private String departmentName;
    private String name;
    private String description;
    private HDPriorityLevel priority;
    private Boolean isActive;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
