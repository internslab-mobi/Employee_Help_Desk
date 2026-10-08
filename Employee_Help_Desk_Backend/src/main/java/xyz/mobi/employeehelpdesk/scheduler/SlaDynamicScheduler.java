package xyz.mobi.employeehelpdesk.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.repository.SlaInstanceRepository;
import xyz.mobi.employeehelpdesk.service.SlaService;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaDynamicScheduler {

    @Qualifier("slaTaskScheduler")
    private final TaskScheduler taskScheduler;
    private final ObjectProvider<SlaService> slaServiceProvider;
    private final SlaInstanceRepository slaInstanceRepository;

    public void scheduleSlaEvent(Long slaInstanceId, Instant nextEventAt) {
        if (slaInstanceId == null || nextEventAt == null) {
            log.warn("Cannot schedule SLA event with null parameters: slaInstanceId={}, nextEventAt={}",
                    slaInstanceId, nextEventAt);
            return;
        }

        Runnable scheduleTask = () -> {
            try {
                log.debug("Scheduling dynamic SLA event for slaInstanceId={} at {}", slaInstanceId, nextEventAt);
                taskScheduler.schedule(() -> {
                    try {
                        slaServiceProvider.getObject().processSlaEvent(slaInstanceId);
                    } catch (Exception ex) {
                        log.error("Unhandled error executing dynamic SLA event for slaInstanceId={}: {}",
                                slaInstanceId, ex.getMessage(), ex);
                    }
                }, nextEventAt);
            } catch (Exception ex) {
                log.error("Failed to schedule in-memory SLA event for slaInstanceId={} at {}: {}. Will be recovered on startup.",
                        slaInstanceId, nextEventAt, ex.getMessage(), ex);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scheduleTask.run();
                }
            });
        } else {
            scheduleTask.run();
        }
    }

    /**
     * Recovers pending SLA events from the database upon application startup.
     * Future events are scheduled in memory with TaskScheduler.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverSlaSchedules() {
        log.info("Starting SLA schedule recovery on application startup...");
        try {
            List<SlaInstance> pendingEvents = slaInstanceRepository.findPendingSlaEvents(
                    List.of(SlaStatus.ACTIVE, SlaStatus.WARNING)
            );

            log.info("Found {} pending SLA event(s) to recover", pendingEvents.size());
            Instant now = Instant.now();

            for (SlaInstance sla : pendingEvents) {
                if (sla.getNextEventAt() == null || sla.getNextEventType() == null) {
                    continue;
                }

                if (sla.getNextEventAt().isAfter(now)) {
                    log.debug("Scheduling future SLA event on startup: id={}, type={}, eventAt={}",
                            sla.getId(), sla.getNextEventType(), sla.getNextEventAt());
                    scheduleSlaEvent(sla.getId(), sla.getNextEventAt());
                } else {
                    log.info("Processing past-due SLA event on startup: id={}, type={}, dueAt={}",
                            sla.getId(), sla.getNextEventType(), sla.getNextEventAt());
                    try {
                        slaServiceProvider.getObject().processSlaEvent(sla.getId());
                    } catch (Exception ex) {
                        log.error("Failed to recover past-due SLA event for id={}: {}",
                                sla.getId(), ex.getMessage(), ex);
                    }
                }
            }
            log.info("SLA schedule recovery completed successfully");
        } catch (Exception ex) {
            log.error("Failed to perform SLA schedule recovery on startup", ex);
        }
    }
}
