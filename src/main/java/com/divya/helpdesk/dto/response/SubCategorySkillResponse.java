package com.divya.helpdesk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubCategorySkillResponse {
    private Long id;
    private Long subCategoryId;
    private String subCategoryName;
    private Long skillId;
    private String skillName;
    private LocalDateTime createdAt;
}
