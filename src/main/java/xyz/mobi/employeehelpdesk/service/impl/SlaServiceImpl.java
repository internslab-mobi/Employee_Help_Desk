package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.service.WorkingCalendarService;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.SlaPolicy;
import xyz.mobi.employeehelpdesk.repository.SlaInstanceRepository;
import xyz.mobi.employeehelpdesk.repository.SlaPolicyRepository;
import xyz.mobi.employeehelpdesk.service.SlaService;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.TicketStatus;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaInstanceRepository slaInstanceRepository;
    private final WorkingCalendarService workingCalendarService;

    /**
     * Starts the INITIAL SLA cycle for a newly created ticket.
     * Uses the full policy duration.
     */
    @Override
    @Transactional
    public void startSla(Ticket ticket) {

        // 1. Validate ticket
        if (ticket.getSubCategory() == null) {
            throw new BadRequestException(
                    "Cannot start SLA without a subcategory"
            );
        }

        // 2. Find active SLA policy
        SlaPolicy policy =
                slaPolicyRepository
                        .findByDepartmentIdAndSubCategoryIdAndIsActiveTrue(
                                ticket.getDepartment().getId(),
                                ticket.getSubCategory().getId()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "No active SLA policy found for subcategory: "
                                                + ticket.getSubCategory().getId()
                                )
                        );

        // 3. Determine start time
        LocalDateTime baseTime = ticket.getCreatedAt() != null
                ? ticket.getCreatedAt()
                : LocalDateTime.now();

        LocalDateTime startAt = workingCalendarService.moveToWorkingTime(baseTime);

        // 4. Cycle number: always 1 for initial SLA
        int cycleNumber = 1;

        // 5. Calculate SLA deadline using full policy duration
        int allocatedMinutes = policy.getDurationMinutes();

        LocalDateTime deadline =
                workingCalendarService.addWorkingMinutes(
                        startAt,
                        allocatedMinutes
                );

        // 6. Calculate warning time
        LocalDateTime warningAt = calculateWarningAt(
                startAt, allocatedMinutes, policy.getWarningMinutes(), policy.getDurationMinutes()
        );

        // 7. Create SLA instance
        SlaInstance slaInstance = SlaInstance.builder()
                .ticket(ticket)
                .slaPolicy(policy)
                .cycleNumber(cycleNumber)
                .allocatedMinutes(allocatedMinutes)
                .slaStartAt(startAt)
                .originalDeadlineAt(deadline)
                .currentDeadlineAt(deadline)
                .warningAt(warningAt)
                .status(SlaStatus.ACTIVE)
                .pausedAt(null)
                .breachedAt(null)
                .build();

        slaInstanceRepository.save(slaInstance);
    }

    /**
     * Starts a REOPENED SLA cycle.
     * Allocation = previous cycle's allocatedMinutes / 2.
     * Warning ratio is preserved from the previous cycle's policy.
     */
    @Override
    @Transactional
    public void startReopenSla(Ticket ticket) {

        if (ticket.getSubCategory() == null) {
            throw new BadRequestException(
                    "Cannot start SLA without a subcategory"
            );
        }

        // 1. Fetch previous SLA cycle (highest cycle number for this ticket)
        SlaInstance previousSla = slaInstanceRepository
                .findTopByTicketIdOrderByCycleNumberDesc(ticket.getId())
                .orElseThrow(() ->
                        new BadRequestException(
                                "Cannot reopen SLA: no previous SLA cycle exists for ticket " + ticket.getId()
                        )
                );

        // 2. Calculate new allocation: half of previous cycle's allocated minutes
        int newAllocatedMinutes = previousSla.getAllocatedMinutes() / 2;
        if (newAllocatedMinutes < 1) {
            newAllocatedMinutes = 1; // Minimum 1 minute
        }

        // 3. Determine cycle number
        int cycleNumber = previousSla.getCycleNumber() + 1;

        // 4. Determine start time from reopen timestamp
        LocalDateTime baseTime = ticket.getReopenedAt() != null
                ? ticket.getReopenedAt()
                : LocalDateTime.now();

        LocalDateTime startAt = workingCalendarService.moveToWorkingTime(baseTime);

        // 5. Calculate deadline
        LocalDateTime deadline =
                workingCalendarService.addWorkingMinutes(
                        startAt,
                        newAllocatedMinutes
                );

        // 6. Calculate warning time preserving the warning ratio from the previous cycle
        LocalDateTime warningAt = calculateWarningAtFromPreviousCycle(
                startAt, newAllocatedMinutes, previousSla
        );

        // 7. Fetch the SLA policy (for reference association, not for duration)
        SlaPolicy policy = previousSla.getSlaPolicy();

        // 8. Create new SLA instance
        SlaInstance slaInstance = SlaInstance.builder()
                .ticket(ticket)
                .slaPolicy(policy)
                .cycleNumber(cycleNumber)
                .allocatedMinutes(newAllocatedMinutes)
                .slaStartAt(startAt)
                .originalDeadlineAt(deadline)
                .currentDeadlineAt(deadline)
                .warningAt(warningAt)
                .status(SlaStatus.ACTIVE)
                .pausedAt(null)
                .breachedAt(null)
                .build();

        slaInstanceRepository.save(slaInstance);
    }

    @Override
    @Transactional
    public SlaInstance pauseSla(Ticket ticket) {
        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticket.getId());

        if (slaInstance == null) {
            return null;
        }

        if (slaInstance.getStatus() == SlaStatus.ACTIVE || slaInstance.getStatus() == SlaStatus.WARNING) {
            slaInstance.setStatus(SlaStatus.PAUSED);
            slaInstance.setPausedAt(LocalDateTime.now());
            return slaInstanceRepository.save(slaInstance);
        }

        return slaInstance;
    }

    @Override
    @Transactional
    public SlaInstance resumeSla(Ticket ticket) {
        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticket.getId());

        if (slaInstance == null) {
            return null;
        }

        if (slaInstance.getStatus() == SlaStatus.PAUSED) {
            LocalDateTime pausedAt = slaInstance.getPausedAt();
            LocalDateTime now = LocalDateTime.now();

            if (pausedAt != null) {
                long pausedWorkingMinutes =
                        workingCalendarService.calculateWorkingMinutes(pausedAt, now);

                if (pausedWorkingMinutes > 0) {
                    slaInstance.setCurrentDeadlineAt(
                            workingCalendarService.addWorkingMinutes(
                                    slaInstance.getCurrentDeadlineAt(),
                                    pausedWorkingMinutes
                            )
                    );

                    if (slaInstance.getWarningAt() != null) {
                        slaInstance.setWarningAt(
                                workingCalendarService.addWorkingMinutes(
                                        slaInstance.getWarningAt(),
                                        pausedWorkingMinutes
                                    )
                        );
                    }
                }
            }

            if (slaInstance.getWarningAt() != null && !now.isBefore(slaInstance.getWarningAt())) {
                slaInstance.setStatus(SlaStatus.WARNING);
            } else {
                slaInstance.setStatus(SlaStatus.ACTIVE);
            }
            slaInstance.setPausedAt(null);
            return slaInstanceRepository.save(slaInstance);
        }

        return slaInstance;
    }

    @Override
    @Transactional
    public SlaInstance completeSla(Ticket ticket) {
        SlaInstance slaInstance = slaInstanceRepository.findLatestByTicketId(ticket.getId());

        if (slaInstance == null) {
            return null;
        }

        if (slaInstance.getStatus() != SlaStatus.COMPLETED
                && slaInstance.getStatus() != SlaStatus.WITHDRAWN
                && slaInstance.getStatus() != SlaStatus.CANCELLED) {
            slaInstance.setStatus(SlaStatus.COMPLETED);
            return slaInstanceRepository.save(slaInstance);
        }

        return slaInstance;
    }

    /**
     * Calculate warning time for initial SLA cycle.
     * warningAt = startAt + (allocated - warningLeadTime)
     * where warningLeadTime comes from the policy.
     */
    private LocalDateTime calculateWarningAt(
            LocalDateTime startAt,
            int allocatedMinutes,
            Integer policyWarningMinutes,
            int policyDurationMinutes
    ) {
        if (policyWarningMinutes == null) {
            return null;
        }

        // warningMinutes in policy = lead time before deadline
        // So warning fires at: allocated - warningMinutes working time from start
        long warningDuration = allocatedMinutes - policyWarningMinutes;
        if (warningDuration < 0) {
            warningDuration = 0;
        }

        return workingCalendarService.addWorkingMinutes(startAt, warningDuration);
    }

    /**
     * Calculate warning time for reopened SLA cycles by preserving the warning ratio
     * from the previous cycle.
     * <p>
     * Example: If previous cycle had allocated=1440, warningLeadTime=360 (25% ratio),
     * and new allocated=720, then new warningLeadTime = 720 * 0.25 = 180.
     */
    private LocalDateTime calculateWarningAtFromPreviousCycle(
            LocalDateTime startAt,
            int newAllocatedMinutes,
            SlaInstance previousSla
    ) {
        if (previousSla.getWarningAt() == null) {
            return null;
        }

        // Derive the warning ratio from the previous cycle's policy
        SlaPolicy prevPolicy = previousSla.getSlaPolicy();
        if (prevPolicy == null || prevPolicy.getWarningMinutes() == null) {
            return null;
        }

        // Warning ratio = warningLeadTime / policyDuration
        // But we use the previous cycle's allocatedMinutes to be safe
        double warningRatio = (double) prevPolicy.getWarningMinutes() / prevPolicy.getDurationMinutes();

        // New warning lead time = new allocation * ratio
        long newWarningLeadTime = Math.round(newAllocatedMinutes * warningRatio);
        if (newWarningLeadTime < 1) {
            newWarningLeadTime = 1;
        }

        long warningDuration = newAllocatedMinutes - newWarningLeadTime;
        if (warningDuration < 0) {
            warningDuration = 0;
        }

        return workingCalendarService.addWorkingMinutes(startAt, warningDuration);
    }
}