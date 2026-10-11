package xyz.mobi.employeehelpdesk.dto.analytics;

public record DepartmentMetricDTO(
        Long departmentId,
        String departmentName,
        long totalTickets,
        long openTickets,
        long resolvedTickets
) {
}
