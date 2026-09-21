package xyz.mobi.employeehelpdesk.service;

import xyz.mobi.employeehelpdesk.dto.subcategoryskill.SubCategorySkillRequest;
import xyz.mobi.employeehelpdesk.dto.subcategoryskill.SubCategorySkillResponse;

import java.util.List;

public interface SubCategorySkillAdminService {

    SubCategorySkillResponse assignSkillToSubCategory(Long subCategoryId, SubCategorySkillRequest request);

    List<SubCategorySkillResponse> getSubCategorySkills(Long subCategoryId);

    void removeSkillFromSubCategory(Long subCategoryId, Long skillId);
}
