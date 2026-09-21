package xyz.mobi.employeehelpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.mobi.employeehelpdesk.dto.agentskill.AgentSkillRequest;
import xyz.mobi.employeehelpdesk.dto.agentskill.AgentSkillResponse;
import xyz.mobi.employeehelpdesk.service.AgentSkillAdminService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminAgentSkillController {

    private final AgentSkillAdminService agentSkillAdminService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/api/admin/agents/{agentId}/skills")
    public ResponseEntity<AgentSkillResponse> assignSkillToAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentSkillRequest request
    ) {
        AgentSkillResponse response = agentSkillAdminService.assignSkillToAgent(agentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/api/admin/agents/{agentId}/skills")
    public ResponseEntity<List<AgentSkillResponse>> getAgentSkills(
            @PathVariable Long agentId
    ) {
        List<AgentSkillResponse> response = agentSkillAdminService.getAgentSkills(agentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/api/admin/agents/{agentId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromAgent(
            @PathVariable Long agentId,
            @PathVariable Long skillId
    ) {
        agentSkillAdminService.removeSkillFromAgent(agentId, skillId);
        return ResponseEntity.noContent().build();
    }
}
