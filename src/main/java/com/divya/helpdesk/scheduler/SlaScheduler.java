package com.divya.helpdesk.scheduler;

import com.divya.helpdesk.service.HDSlaInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final HDSlaInstanceService slaInstanceService;

    @Scheduled(fixedRate = 60000)
    public void checkSla() {
        try {
            slaInstanceService.checkSlaInstances();
        } catch (Exception e) {
            log.error("Error during SLA check execution: {}", e.getMessage(), e);
        }
    }
}
