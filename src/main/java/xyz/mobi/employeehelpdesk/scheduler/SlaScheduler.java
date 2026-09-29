package xyz.mobi.employeehelpdesk.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.mobi.employeehelpdesk.service.SlaService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private final SlaService slaService;

    @Scheduled(fixedDelayString = "${helpdesk.sla.scheduler-interval-ms:60000}")
    public void evaluateSlas() {
        log.info("Starting SLA evaluation run");
        try {
            // Must process breach before warning
            slaService.processSlaBreaches();
            slaService.processSlaWarnings();
        } catch (Exception e) {
            log.error("Unexpected error occurred during SLA evaluation cycle", e);
        }
        log.debug("Completed SLA evaluation run");
    }
}
