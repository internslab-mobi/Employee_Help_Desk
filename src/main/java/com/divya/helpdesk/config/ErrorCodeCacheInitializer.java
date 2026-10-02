package com.divya.helpdesk.config;

import com.divya.helpdesk.service.ErrorCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErrorCodeCacheInitializer {

    private final ErrorCodeService errorCodeService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        errorCodeService.loadAllErrorCodes();
    }
}
