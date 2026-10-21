package xyz.mobi.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DistributionItemDTO(
        String key,
        String label,
        long count,
        double percentage
) {
}
