package xyz.mobi.notificationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.mobi.notificationservice.dto.EmailRequestDTO;
import xyz.mobi.notificationservice.dto.EmailResponseDTO;
import xyz.mobi.notificationservice.service.EmailService;

@Slf4j
@RestController
@RequestMapping({"/emails", "/notifications", "/helpdesk/notifications"})
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping({"", "/send", "/email"})
    public ResponseEntity<EmailResponseDTO> sendEmail(
            @Valid @RequestBody EmailRequestDTO request
    ) {
        log.info("Received email dispatch request for recipient: {}", request.to());
        emailService.sendEmail(request);
        return ResponseEntity.ok(EmailResponseDTO.success(request.to(), "Email notification accepted for delivery"));
    }
}
