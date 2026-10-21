package xyz.mobi.employeehelpdesk.dto.slapolicy;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SlaPolicyResponseDTO(
        Long id,
        Long departmentId,
        String departmentName,
        Long subCategoryId,
        String subCategoryName,
        Integer durationMinutes,
        Integer warningMinutes,
        Boolean isActive
) {
}
