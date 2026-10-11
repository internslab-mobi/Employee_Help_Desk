package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmployeeStatsDTO(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long onLeaveEmployees
) {
}
