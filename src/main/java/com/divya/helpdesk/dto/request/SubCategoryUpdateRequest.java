package com.divya.helpdesk.dto.request;

import com.divya.helpdesk.enums.HDPriorityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubCategoryUpdateRequest {

    @NotBlank(message = "SubCategory name is required")
    @Size(max = 100, message = "SubCategory name must not exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Priority is required")
    private HDPriorityLevel priority;

    private Boolean isActive;
}
