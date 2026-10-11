package xyz.mobi.employeehelpdesk.dto.analytics;

public record TrendItemDTO(
        String period,
        long createdCount,
        long resolvedCount
) {
}
