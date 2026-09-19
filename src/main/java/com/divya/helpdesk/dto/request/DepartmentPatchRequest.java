package com.divya.helpdesk.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentPatchRequest {
    private String code;
    private String name;
    private String description;
    private Long businessCalendarId;
    private Boolean isActive;
}
