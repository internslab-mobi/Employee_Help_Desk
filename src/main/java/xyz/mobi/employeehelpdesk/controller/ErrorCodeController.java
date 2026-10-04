package xyz.mobi.employeehelpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.employeehelpdesk.service.helperservice.ErrorCodeCacheService;

import java.util.Map;

@RestController
@RequestMapping("/error-codes")
@RequiredArgsConstructor
public class ErrorCodeController {

    private final ErrorCodeCacheService errorCodeCacheService;

    @Operation(summary = "Manually refresh error-code mappings cache from the database (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshErrorCodeCache() {
        errorCodeCacheService.refreshCache();
        return ResponseEntity.ok(Map.of("message", "Error-code mappings cache refreshed successfully"));
    }
}
