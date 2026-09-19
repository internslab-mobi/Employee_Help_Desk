package com.divya.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubCategorySkillAssignRequest {

    @NotNull(message = "SubCategory ID is required")
    private Long subCategoryId;

    @NotNull(message = "Skill ID is required")
    private Long skillId;
}
