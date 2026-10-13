package xyz.mobi.employeehelpdesk.dto.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryResponseDTO(
        Long id,
        Long departmentId,
        String departmentName,
        String name,
        String description,
        Boolean isActive
) {
}
