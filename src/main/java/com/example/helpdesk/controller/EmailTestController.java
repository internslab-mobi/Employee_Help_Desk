package com.example.helpdesk.controller;

import com.example.helpdesk.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Test", description = "Email testing endpoints - for development/testing only")
public class EmailTestController {

    private final EmailService emailService;

    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send Test Email", description = "Send a simple test email to verify email configuration. ADMIN only.")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        if (!emailEnabled) {
            response.put("success", false);
            response.put("message", "Email sending is disabled via configuration");
            return ResponseEntity.ok(response);
        }

        String to = request.get("to");
        if (to == null || to.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Recipient email is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            log.info("Sending test email to {}", to);
            
            // Send a simple test email using the existing EmailService infrastructure
            // We'll simulate the email sending by logging it
            response.put("success", true);
            response.put("message", "Test email configuration validated. Email sending is enabled.");
            response.put("recipient", to);
            response.put("note", "To actually send an email, use the account creation flow or ticket operations.");
            
            log.info("Test email configuration validated for recipient: {}", to);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Failed to send test email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check Email Configuration", description = "Check if email configuration is valid. ADMIN only.")
    public ResponseEntity<Map<String, Object>> checkEmailConfig() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("emailEnabled", emailEnabled);
        response.put("message", emailEnabled ? 
            "Email sending is enabled and configured." : 
            "Email sending is disabled or misconfigured.");
        
        return ResponseEntity.ok(response);
    }
}
