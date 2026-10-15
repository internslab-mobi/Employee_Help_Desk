package xyz.mobi.employeehelpdesk.dto.analytics;

public record DistributionItemDTO(
        String key,
        String label,
        long count,
        double percentage
) {
}
