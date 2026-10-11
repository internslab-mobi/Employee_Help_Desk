package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrendItemDTO(
        String period,
        long createdCount,
        long resolvedCount
) {
}
