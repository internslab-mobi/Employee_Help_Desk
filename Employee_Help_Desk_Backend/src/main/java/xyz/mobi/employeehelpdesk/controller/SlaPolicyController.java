package xyz.mobi.employeehelpdesk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponseDTO;
import xyz.mobi.employeehelpdesk.service.SlaService;
import xyz.mobi.employeehelpdesk.dto.slapolicy.CreateSlaPolicyRequestDTO;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/helpdesk/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaService slaService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<SlaPolicyResponseDTO> getSlaPolicyById(@PathVariable Long id) {
        SlaPolicyResponseDTO response = slaService.getSlaPolicyById(id);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<SlaPolicyResponseDTO>> getSlaPolicies(
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(slaService.getSlaPolicies(departmentId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SlaPolicyResponseDTO> createSlaPolicy(
            @Valid @RequestBody CreateSlaPolicyRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slaService.createSlaPolicy(request));
    }

}
