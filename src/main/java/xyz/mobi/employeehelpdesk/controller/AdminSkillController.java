package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.skill.CreateSkillRequest;
import xyz.mobi.employeehelpdesk.dto.skill.SkillResponse;
import xyz.mobi.employeehelpdesk.dto.skill.UpdateSkillRequest;
import xyz.mobi.employeehelpdesk.service.SkillAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/skills")
@RequiredArgsConstructor
public class AdminSkillController {

    private final SkillAdminService skillAdminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(
            @Valid @RequestBody CreateSkillRequest request
    ) {
        SkillResponse response = skillAdminService.createSkill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<SkillResponse>> getAllSkills(
            @RequestParam(required = false) Boolean activeOnly
    ) {
        List<SkillResponse> response = skillAdminService.getAllSkills(activeOnly);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkillById(@PathVariable Long id) {
        SkillResponse response = skillAdminService.getSkillById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSkillRequest request
    ) {
        SkillResponse response = skillAdminService.updateSkill(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillAdminService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}
