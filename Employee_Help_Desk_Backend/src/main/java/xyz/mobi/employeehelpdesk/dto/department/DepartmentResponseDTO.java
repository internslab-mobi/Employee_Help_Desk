package xyz.mobi.employeehelpdesk.dto.department;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepartmentResponseDTO(
        Long id,
        String code,
        String name,
        String description,
        Boolean isActive,
        String timezone
) {
}
