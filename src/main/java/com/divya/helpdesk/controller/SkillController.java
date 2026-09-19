package com.divya.helpdesk.controller;

import com.divya.helpdesk.dto.request.AgentSkillAssignRequest;
import com.divya.helpdesk.dto.request.SkillCreateRequest;
import com.divya.helpdesk.dto.request.SkillUpdateRequest;
import com.divya.helpdesk.dto.request.SubCategorySkillAssignRequest;
import com.divya.helpdesk.dto.response.AgentSkillResponse;
import com.divya.helpdesk.dto.response.SkillResponse;
import com.divya.helpdesk.dto.response.SubCategorySkillResponse;
import com.divya.helpdesk.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    // --- Skill Core Endpoints ---

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(@Valid @RequestBody SkillCreateRequest request) {
        SkillResponse response = skillService.createSkill(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.getSkillById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillUpdateRequest request) {
        return ResponseEntity.ok(skillService.updateSkill(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    // --- Agent Skill Endpoints ---

    @PostMapping("/agents/assign")
    public ResponseEntity<AgentSkillResponse> assignSkillToAgent(@Valid @RequestBody AgentSkillAssignRequest request) {
        AgentSkillResponse response = skillService.assignSkillToAgent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/agents/{agentId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromAgent(@PathVariable Long agentId, @PathVariable Long skillId) {
        skillService.removeSkillFromAgent(agentId, skillId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agents/{agentId}")
    public ResponseEntity<List<AgentSkillResponse>> getSkillsByAgent(@PathVariable Long agentId) {
        return ResponseEntity.ok(skillService.getSkillsByAgent(agentId));
    }

    // --- SubCategory Skill Endpoints ---

    @PostMapping("/subcategories/assign")
    public ResponseEntity<SubCategorySkillResponse> assignSkillToSubCategory(@Valid @RequestBody SubCategorySkillAssignRequest request) {
        SubCategorySkillResponse response = skillService.assignSkillToSubCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/subcategories/{subcatId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromSubCategory(@PathVariable Long subcatId, @PathVariable Long skillId) {
        skillService.removeSkillFromSubCategory(subcatId, skillId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subcategories/{subcatId}")
    public ResponseEntity<List<SubCategorySkillResponse>> getSkillsBySubCategory(@PathVariable Long subcatId) {
        return ResponseEntity.ok(skillService.getSkillsBySubCategory(subcatId));
    }

    @GetMapping("/{skillId}/subcategories")
    public ResponseEntity<List<SubCategorySkillResponse>> getSubCategoriesBySkill(@PathVariable Long skillId) {
        return ResponseEntity.ok(skillService.getSubCategoriesBySkill(skillId));
    }
}
