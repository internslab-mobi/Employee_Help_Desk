package xyz.mobi.employeehelpdesk.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.repository.SlaInstanceRepository;
import xyz.mobi.employeehelpdesk.service.EmailService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.SlaEvaluationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SlaEvaluationServiceImpl implements SlaEvaluationService {

    private final SlaInstanceRepository slaInstanceRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final int batchSize;

    public SlaEvaluationServiceImpl(
            SlaInstanceRepository slaInstanceRepository,
            NotificationService notificationService,
            EmailService emailService,
            @Value("${helpdesk.sla.batch-size:50}") int batchSize
    ) {
        this.slaInstanceRepository = slaInstanceRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.batchSize = batchSize;
    }

    /**
     * Sweeps ALL eligible SLA instances for breaches using keyset pagination.
     * Loops until no more candidates remain.
     */
    @Override
    @Transactional(readOnly = true)
    public void processSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();
        List<SlaStatus> eligibleStatuses = List.of(SlaStatus.ACTIVE, SlaStatus.WARNING);
        long lastProcessedId = 0L;
        int totalProcessed = 0;

        while (true) {
            List<SlaInstance> batch = slaInstanceRepository.findBreachCandidatesAfter(
                    eligibleStatuses,
                    now,
                    lastProcessedId,
                    PageRequest.of(0, batchSize)
            );

            if (batch.isEmpty()) {
                break;
            }

            for (SlaInstance sla : batch) {
                try {
                    evaluateBreach(sla.getId());
                    totalProcessed++;
                } catch (Exception ex) {
                    log.error("Failed to evaluate breach for SLA id={}: {}", sla.getId(), ex.getMessage(), ex);
                }
                lastProcessedId = sla.getId();
            }

            if (batch.size() < batchSize) {
                break;
            }
        }

        if (totalProcessed > 0) {
            log.info("SLA breach sweep complete: processed {} instance(s)", totalProcessed);
        }
    }

    /**
     * Sweeps ALL eligible SLA instances for warnings using keyset pagination.
     * Loops until no more candidates remain.
     */
    @Override
    @Transactional(readOnly = true)
    public void processSlaWarnings() {
        LocalDateTime now = LocalDateTime.now();
        long lastProcessedId = 0L;
        int totalProcessed = 0;

        while (true) {
            List<SlaInstance> batch = slaInstanceRepository.findWarningCandidatesAfter(
                    SlaStatus.ACTIVE,
                    now,
                    lastProcessedId,
                    PageRequest.of(0, batchSize)
            );

            if (batch.isEmpty()) {
                break;
            }

            for (SlaInstance sla : batch) {
                try {
                    evaluateWarning(sla.getId());
                    totalProcessed++;
                } catch (Exception ex) {
                    log.error("Failed to evaluate warning for SLA id={}: {}", sla.getId(), ex.getMessage(), ex);
                }
                lastProcessedId = sla.getId();
            }

            if (batch.size() < batchSize) {
                break;
            }
        }

        if (totalProcessed > 0) {
            log.info("SLA warning sweep complete: processed {} instance(s)", totalProcessed);
        }
    }

    /**
     * Atomically transitions a single SLA to BREACHED if still eligible.
     * Runs in its own transaction to ensure individual failures don't roll back the sweep.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateBreach(Long slaInstanceId) {
        LocalDateTime now = LocalDateTime.now();
        List<SlaStatus> expectedStatuses = List.of(SlaStatus.ACTIVE, SlaStatus.WARNING);

        int updated = slaInstanceRepository.updateStatusToBreachedIfEligible(
                slaInstanceId,
                SlaStatus.BREACHED,
                expectedStatuses,
                now
        );

        if (updated > 0) {
            log.info("SLA breached: id={}", slaInstanceId);

            SlaInstance sla = slaInstanceRepository.findById(slaInstanceId).orElse(null);
            if (sla != null) {
                sendBreachNotifications(sla);
            }
        }
    }

    /**
     * Atomically transitions a single SLA to WARNING if still eligible.
     * Runs in its own transaction to ensure individual failures don't roll back the sweep.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateWarning(Long slaInstanceId) {
        LocalDateTime now = LocalDateTime.now();

        int updated = slaInstanceRepository.updateStatusToWarningIfEligible(
                slaInstanceId,
                SlaStatus.WARNING,
                SlaStatus.ACTIVE,
                now
        );

        if (updated > 0) {
            log.info("SLA warning triggered: id={}", slaInstanceId);

            SlaInstance sla = slaInstanceRepository.findById(slaInstanceId).orElse(null);
            if (sla != null) {
                sendWarningNotifications(sla);
            }
        }
    }

    private void sendBreachNotifications(SlaInstance sla) {
        try {
            if (sla.getTicket() == null) {
                return;
            }

            String ticketNumber = sla.getTicket().getTicketNumber() != null
                    ? sla.getTicket().getTicketNumber()
                    : ("#" + sla.getTicket().getId());
            String message = "SLA breached for ticket " + ticketNumber + ".";
            String subject = "SLA Breach – " + ticketNumber;

            // Notify assigned agent
            if (sla.getTicket().getAssignedAgent() != null
                    && sla.getTicket().getAssignedAgent().getEmployee() != null) {
                notificationService.sendNotification(
                        sla.getTicket().getAssignedAgent().getEmployee(),
                        sla.getTicket(),
                        NotificationType.SLA_BREACHED,
                        subject,
                        message
                );
                emailService.sendNotificationEmail(
                        sla.getTicket().getAssignedAgent().getEmployee(),
                        subject,
                        message
                );
            }

            // Notify requester
            if (sla.getTicket().getRequester() != null) {
                notificationService.sendNotification(
                        sla.getTicket().getRequester(),
                        sla.getTicket(),
                        NotificationType.SLA_BREACHED,
                        subject,
                        message
                );
            }
        } catch (Exception ex) {
            log.error("Failed to send breach notifications for SLA id={}: {}",
                    sla.getId(), ex.getMessage(), ex);
        }
    }

    private void sendWarningNotifications(SlaInstance sla) {
        try {
            if (sla.getTicket() == null) {
                return;
            }

            String ticketNumber = sla.getTicket().getTicketNumber() != null
                    ? sla.getTicket().getTicketNumber()
                    : ("#" + sla.getTicket().getId());
            String message = "SLA warning for ticket " + ticketNumber
                    + ". Deadline approaching.";
            String subject = "SLA Warning – " + ticketNumber;

            // Notify assigned agent
            if (sla.getTicket().getAssignedAgent() != null
                    && sla.getTicket().getAssignedAgent().getEmployee() != null) {
                notificationService.sendNotification(
                        sla.getTicket().getAssignedAgent().getEmployee(),
                        sla.getTicket(),
                        NotificationType.SLA_WARNING,
                        subject,
                        message
                );
                emailService.sendNotificationEmail(
                        sla.getTicket().getAssignedAgent().getEmployee(),
                        subject,
                        message
                );
            }
        } catch (Exception ex) {
            log.error("Failed to send warning notifications for SLA id={}: {}",
                    sla.getId(), ex.getMessage(), ex);
        }
    }
}
