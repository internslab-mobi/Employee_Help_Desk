package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DepartmentMetricDTO(
        Long departmentId,
        String departmentName,
        long totalTickets,
        long openTickets,
        long resolvedTickets
) {
}
