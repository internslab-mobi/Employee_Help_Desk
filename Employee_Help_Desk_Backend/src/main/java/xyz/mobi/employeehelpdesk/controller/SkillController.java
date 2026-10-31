package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.skill.AssignSkillsRequestDTO;
import xyz.mobi.employeehelpdesk.dto.skill.CreateSkillRequestDTO;
import xyz.mobi.employeehelpdesk.dto.skill.SkillResponseDTO;
import xyz.mobi.employeehelpdesk.service.SkillService;

import java.util.List;

@Tag(name = "Skills", description = "Endpoints for managing skills, agent skills, and subcategory skills")
@RestController
@RequestMapping("/helpdesk/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Get all skills")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<SkillResponseDTO>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @Operation(summary = "Create a new skill")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SkillResponseDTO> createSkill(@Valid @RequestBody CreateSkillRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.createSkill(request));
    }

    @Operation(summary = "Get skills for an agent")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<SkillResponseDTO>> getAgentSkills(@PathVariable Long agentId) {
        return ResponseEntity.ok(skillService.getAgentSkills(agentId));
    }

    @Operation(summary = "Assign skills to an agent")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/agent/{agentId}")
    public ResponseEntity<List<SkillResponseDTO>> assignAgentSkills(
            @PathVariable Long agentId,
            @RequestBody AssignSkillsRequestDTO request
    ) {
        return ResponseEntity.ok(skillService.assignAgentSkills(agentId, request));
    }

    @Operation(summary = "Get required skills for a subcategory")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/subcategory/{subCategoryId}")
    public ResponseEntity<List<SkillResponseDTO>> getSubCategorySkills(@PathVariable Long subCategoryId) {
        return ResponseEntity.ok(skillService.getSubCategorySkills(subCategoryId));
    }

    @Operation(summary = "Assign required skills to a subcategory")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/subcategory/{subCategoryId}")
    public ResponseEntity<List<SkillResponseDTO>> assignSubCategorySkills(
            @PathVariable Long subCategoryId,
            @RequestBody AssignSkillsRequestDTO request
    ) {
        return ResponseEntity.ok(skillService.assignSubCategorySkills(subCategoryId, request));
    }
}
