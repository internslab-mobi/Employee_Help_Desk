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
public class DepartmentResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private Long businessCalendarId;
    private String businessCalendarName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
