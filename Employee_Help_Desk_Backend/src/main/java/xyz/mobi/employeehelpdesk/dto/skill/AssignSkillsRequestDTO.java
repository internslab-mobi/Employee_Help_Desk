package xyz.mobi.employeehelpdesk.dto.skill;

import java.util.List;

public record AssignSkillsRequestDTO(
        List<Long> skillIds
) {}
