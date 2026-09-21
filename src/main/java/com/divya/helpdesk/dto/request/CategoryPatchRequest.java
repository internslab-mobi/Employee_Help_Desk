package com.divya.helpdesk.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPatchRequest {
    private String name;
    private String description;
    private Boolean isActive;
}
