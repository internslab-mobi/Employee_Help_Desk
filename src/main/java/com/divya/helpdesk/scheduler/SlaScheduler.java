package com.divya.helpdesk.scheduler;

import com.divya.helpdesk.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlaScheduler {
    private final SlaService slaService;

    @Scheduled(fixedRate = 60000)
    public void checkSla(){
        slaService.checkSlaInstances();
    }
}
