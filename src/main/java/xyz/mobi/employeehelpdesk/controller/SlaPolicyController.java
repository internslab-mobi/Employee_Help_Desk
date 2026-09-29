package xyz.mobi.employeehelpdesk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponse;
import xyz.mobi.employeehelpdesk.service.SlaService;

@RestController
@RequestMapping("/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaService slaService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<SlaPolicyResponse> getSlaPolicyById(@PathVariable Long id) {
        SlaPolicyResponse response = slaService.getSlaPolicyById(id);
        return ResponseEntity.ok(response);
    }

}
