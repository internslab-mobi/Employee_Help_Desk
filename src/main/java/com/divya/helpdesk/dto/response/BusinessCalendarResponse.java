package com.divya.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCalendarResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String timezone;
    private Boolean isDefault;
    private Boolean isActive;
    private List<BusinessHoursResponse> businessHours;
    private List<HolidayResponse> holidays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
