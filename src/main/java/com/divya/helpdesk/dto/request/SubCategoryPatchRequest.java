package com.divya.helpdesk.dto.request;

import com.divya.helpdesk.enums.HDPriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubCategoryPatchRequest {
    private String name;
    private String description;
    private HDPriorityLevel priority;
    private Boolean isActive;
}
