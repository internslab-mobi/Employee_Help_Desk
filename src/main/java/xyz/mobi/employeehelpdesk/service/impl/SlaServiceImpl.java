package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponse;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.SlaPolicy;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.InvalidStateException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentManagerRepository;
import xyz.mobi.employeehelpdesk.repository.SlaInstanceRepository;
import xyz.mobi.employeehelpdesk.repository.SlaPolicyRepository;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.SlaService;
import xyz.mobi.employeehelpdesk.service.WorkingCalendarService;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaInstanceRepository slaInstanceRepository;
    private final WorkingCalendarService workingCalendarService;
    private final AuthService authService;
    private final DepartmentManagerRepository departmentManagerRepository;
    private final NotificationService notificationService;
    private final int batchSize = 50;

    /**
     * Starts the INITIAL SLA cycle for a newly created ticket.
     * Uses the full policy duration.
     */
    @Override
    @Transactional
    public SlaInstance startSla(Ticket ticket) {

        // 1. Validate ticket
        if (ticket.getSubCategory() == null) {
            throw new BadRequestException(
                    "Cannot start SLA without a subcategory"
            );
        }
        if (ticket.getDepartment() == null) {
            throw new BadRequestException(
                    "Cannot start SLA without a department"
            );
        }

        Department department = ticket.getDepartment();
        Long departmentId = department.getId();
        ZoneId departmentZone = resolveDepartmentZone(department);

        // 2. Find active SLA policy
        SlaPolicy policy =
                slaPolicyRepository
                        .findByDepartmentIdAndSubCategoryIdAndIsActiveTrue(
                                departmentId,
                                ticket.getSubCategory().getId()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "No active SLA policy found for subcategory: "
                                                + ticket.getSubCategory().getId()
                                )
                        );

        // 3. Determine start time
        Instant baseInstant = ticket.getCreatedAt() != null
                ? ticket.getCreatedAt()
                : Instant.now();

        Instant startAt = workingCalendarService.moveToWorkingTime(baseInstant, departmentId, departmentZone);

        // 4. Cycle number: always 1 for initial SLA
        int cycleNumber = 1;

        // 5. Calculate SLA deadline using full policy duration
        int allocatedMinutes = policy.getDurationMinutes();

        Instant deadline =
                workingCalendarService.addWorkingMinutes(
                        startAt,
                        allocatedMinutes,
                        departmentId,
                        departmentZone
                );

        // 6. Calculate warning time
        Instant warningAt = calculateWarningAt(
                startAt, allocatedMinutes, policy.getWarningMinutes(), policy.getDurationMinutes(),
                departmentId, departmentZone
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

        return slaInstanceRepository.save(slaInstance);
    }

    /**
     * Starts a REOPENED SLA cycle.
     * Allocation = previous cycle's allocatedMinutes / 2.
     * Warning ratio is preserved from the previous cycle's policy.
     */
    @Override
    @Transactional
    public SlaInstance startReopenSla(Ticket ticket) {

        if (ticket.getSubCategory() == null) {
            throw new BadRequestException(
                    "Cannot start SLA without a subcategory"
            );
        }
        if (ticket.getDepartment() == null) {
            throw new BadRequestException(
                    "Cannot start SLA without a department"
            );
        }

        Department department = ticket.getDepartment();
        Long departmentId = department.getId();
        ZoneId departmentZone = resolveDepartmentZone(department);

        // 1. Fetch previous SLA cycle (highest cycle number for this ticket)
        SlaInstance previousSla = slaInstanceRepository
                .findTopByTicketIdOrderByCycleNumberDesc(ticket.getId())
                .orElseThrow(() ->
                        new InvalidStateException(
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
        Instant baseInstant = ticket.getReopenedAt() != null
                ? ticket.getReopenedAt()
                : Instant.now();

        Instant startAt = workingCalendarService.moveToWorkingTime(baseInstant, departmentId, departmentZone);

        // 5. Calculate deadline
        Instant deadline =
                workingCalendarService.addWorkingMinutes(
                        startAt,
                        newAllocatedMinutes,
                        departmentId,
                        departmentZone
                );

        // 6. Calculate warning time preserving the warning ratio from the previous cycle
        Instant warningAt = calculateWarningAtFromPreviousCycle(
                startAt, newAllocatedMinutes, previousSla, departmentId, departmentZone
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

        return slaInstanceRepository.save(slaInstance);
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
            slaInstance.setPausedAt(Instant.now());
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
            Instant pausedAtInstant = slaInstance.getPausedAt();
            Instant now = Instant.now();

            if (pausedAtInstant != null) {
                Department department = ticket.getDepartment();
                Long departmentId = department != null ? department.getId() : null;
                ZoneId departmentZone = resolveDepartmentZone(department);

                long pausedWorkingMinutes =
                        workingCalendarService.calculateWorkingMinutes(
                                pausedAtInstant,
                                now,
                                departmentId,
                                departmentZone
                        );

                if (pausedWorkingMinutes > 0) {
                    Instant currentDeadline = slaInstance.getCurrentDeadlineAt();
                    slaInstance.setCurrentDeadlineAt(
                            workingCalendarService.addWorkingMinutes(
                                    currentDeadline,
                                    pausedWorkingMinutes,
                                    departmentId,
                                    departmentZone
                            )
                    );

                    if (slaInstance.getWarningAt() != null) {
                        Instant currentWarning = slaInstance.getWarningAt();
                        slaInstance.setWarningAt(
                                workingCalendarService.addWorkingMinutes(
                                        currentWarning,
                                        pausedWorkingMinutes,
                                        departmentId,
                                        departmentZone
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
    private Instant calculateWarningAt(
            Instant startAt,
            int allocatedMinutes,
            Integer policyWarningMinutes,
            int policyDurationMinutes,
            Long departmentId,
            ZoneId departmentZone
    ) {
        if (policyWarningMinutes == null) {
            return null;
        }

        // warningMinutes in policy = lead time before deadline
        // So warning fires at: allocated - warningMinutes working time from start
        long warningDuration = (long) allocatedMinutes - policyWarningMinutes;
        if (warningDuration < 0) {
            warningDuration = 0;
        }

        return workingCalendarService.addWorkingMinutes(startAt, warningDuration, departmentId, departmentZone);
    }

    /**
     * Calculate warning time for reopened SLA cycles by preserving the warning ratio
     * from the previous cycle.
     * <p>
     * Example: If previous cycle had allocated=1440, warningLeadTime=360 (25% ratio),
     * and new allocated=720, then new warningLeadTime = 720 * 0.25 = 180.
     */
    private Instant calculateWarningAtFromPreviousCycle(
            Instant startAt,
            int newAllocatedMinutes,
            SlaInstance previousSla,
            Long departmentId,
            ZoneId departmentZone
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

        long warningDuration = (long) newAllocatedMinutes - newWarningLeadTime;
        if (warningDuration < 0) {
            warningDuration = 0;
        }

        return workingCalendarService.addWorkingMinutes(startAt, warningDuration, departmentId, departmentZone);
    }

    private ZoneId resolveDepartmentZone(Department department) {
        if (department == null) {
            throw new BadRequestException("Cannot calculate SLA without a department");
        }
        String tz = department.getTimezone();
        if (tz == null || tz.isBlank()) {
            throw new InvalidStateException("Department " + department.getId() + " does not have a configured timezone");
        }
        try {
            return ZoneId.of(tz.trim());
        } catch (Exception e) {
            throw new InvalidStateException("Invalid timezone '" + tz + "' configured for department " + department.getId());
        }
    }




    //crud sla


    @Override
    @Transactional(readOnly = true)
    public SlaPolicyResponse getSlaPolicyById(Long id) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SlaPolicy not found with id: " + id));
        return toResponse(policy);
    }


    private void validateTiming(Integer durationMinutes, Integer warningMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BadRequestException("Duration must be greater than 0");
        }
        if (warningMinutes != null) {
            if (warningMinutes <= 0) {
                throw new BadRequestException("Warning duration must be greater than 0");
            }
            if (warningMinutes >= durationMinutes) {
                throw new BadRequestException("Warning duration (" + warningMinutes + ") must be less than duration (" + durationMinutes + ")");
            }
        }
    }

    private void checkDepartmentAccess(Long departmentId) {
        UserRole role = authService.getCurrentUserRole();
        if (role == UserRole.ADMIN) {
            return;
        }
        if (role == UserRole.MANAGER) {
            Long currentEmpId = authService.getCurrentEmployeeId();
            if (!isManagerOfDepartment(currentEmpId, departmentId)) {
                throw new AccessDeniedException("Access denied: You are not a manager of department " + departmentId);
            }
            return;
        }
        throw new AccessDeniedException("Access denied: Insufficient permissions");
    }

    private SlaPolicyResponse toResponse(SlaPolicy policy) {
        return SlaPolicyResponse.builder()
                .id(policy.getId())
                .departmentId(policy.getDepartment().getId())
                .departmentName(policy.getDepartment().getName())
                .subCategoryId(policy.getSubCategory().getId())
                .subCategoryName(policy.getSubCategory().getName())
                .durationMinutes(policy.getDurationMinutes())
                .warningMinutes(policy.getWarningMinutes())
                .isActive(policy.getIsActive())
                .build();
    }


    private boolean isManagerOfDepartment(
            Long employeeId,
            Long departmentId
    ) {

        if (employeeId == null || departmentId == null) {
            return false;
        }

        return departmentManagerRepository
                .existsByEmployeeIdAndDepartmentId(
                        employeeId,
                        departmentId
                );
    }



    //sla evaluation

    /**
     * Sweeps ALL eligible SLA instances for breaches using keyset pagination.
     * Loops until no more candidates remain.
     */
    @Override
    @Transactional
    public void processSlaBreaches() {
        Instant now = Instant.now();
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
    @Transactional
    public void processSlaWarnings() {
        Instant now = Instant.now();
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
        Instant now = Instant.now();
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
        Instant now = Instant.now();

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
                        sla.getTicket().getAssignedAgent().getEmployee().getId(),
                        sla.getTicket().getId(),
                        NotificationType.SLA_BREACHED,
                        subject,
                        message
                );
            }

            // Notify requester
            if (sla.getTicket().getRequester() != null) {
                notificationService.sendNotification(
                        sla.getTicket().getRequester().getId(),
                        sla.getTicket().getId(),
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
                        sla.getTicket().getAssignedAgent().getEmployee().getId(),
                        sla.getTicket().getId(),
                        NotificationType.SLA_WARNING,
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