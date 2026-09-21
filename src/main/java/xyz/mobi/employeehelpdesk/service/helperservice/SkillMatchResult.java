package xyz.mobi.employeehelpdesk.service.helperservice;

public record SkillMatchResult(
        int matchedSkillCount,
        int requiredSkillCount
) {
}