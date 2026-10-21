package com.example.helpdesk.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    @PostMapping("/test-event")
    public ResponseEntity<String> createTestEvent() {

        return ResponseEntity.ok(
                googleCalendarService.createTestEvent()
        );
    }
}