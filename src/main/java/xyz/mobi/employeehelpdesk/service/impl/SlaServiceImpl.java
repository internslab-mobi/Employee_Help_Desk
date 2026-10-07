package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.slapolicy.SlaPolicyResponseDTO;
import xyz.mobi.employeehelpdesk.entity.Department;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.SlaPolicy;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.NotificationType;
import xyz.mobi.employeehelpdesk.entity.enums.SlaEventType;
import xyz.mobi.employeehelpdesk.entity.enums.SlaStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.exception.InvalidStateException;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.DepartmentManagerRepository;
import xyz.mobi.employeehelpdesk.repository.SlaInstanceRepository;
import xyz.mobi.employeehelpdesk.repository.SlaPolicyRepository;
import xyz.mobi.employeehelpdesk.scheduler.SlaDynamicScheduler;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.service.NotificationService;
import xyz.mobi.employeehelpdesk.service.SlaService;
import xyz.mobi.employeehelpdesk.service.WorkingCalendarService;

import java.time.Instant;
import java.time.ZoneId;

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
    private final SlaDynamicScheduler slaDynamicScheduler;

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

        // 4. cycle number 1 for initial SLA
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

        // 7. Determine initial event
        SlaEventType nextEventType;
        Instant nextEventAt;

        if (warningAt != null && warningAt.isBefore(deadline)) {
            nextEventType = SlaEventType.WARNING;
            nextEventAt = warningAt;
        } else {
            nextEventType = SlaEventType.BREACH;
            nextEventAt = deadline;
        }

        // 8. Create SLA instance
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
                .nextEventType(nextEventType)
                .nextEventAt(nextEventAt)
                .pausedAt(null)
                .breachedAt(null)
                .build();

        SlaInstance savedSla = slaInstanceRepository.save(slaInstance);
        slaDynamicScheduler.scheduleSlaEvent(savedSla.getId(), savedSla.getNextEventAt());

        log.info("SLA started: ticketId={}, slaInstanceId={}, allocatedMinutes={}, deadline={}, nextEventType={}, nextEventAt={}",
                ticket.getId(), savedSla.getId(), allocatedMinutes, deadline, nextEventType, nextEventAt);

        return savedSla;
    }

    /**
     * Starts a REOPENED SLA cycle.
     * Allocation = previous cycle's allocatedMinutes / 2.
     * Warning ratio is preserved from the previous cycle's policy.
     */
    @Override
    @Transactional
    public SlaInstance startReopenSla(Ticket ticket) {
        Instant baseInstant = ticket.getReopenedAt() != null
                ? ticket.getReopenedAt()
                : Instant.now();
        return createReducedSlaCycle(ticket, baseInstant, "Reopened");
    }

    /**
     * Starts a BREACH RECOVERY SLA cycle for assigned/reassigned tickets.
     * Allocation = previous cycle's allocatedMinutes / 2.
     * Warning ratio is preserved from the previous cycle's policy.
     */
    @Override
    @Transactional
    public SlaInstance startBreachRecoverySla(Ticket ticket) {
        return createReducedSlaCycle(ticket, Instant.now(), "Breach recovery");
    }

    private SlaInstance createReducedSlaCycle(Ticket ticket, Instant baseInstant, String cycleType) {
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
                                "Cannot " + (cycleType.equalsIgnoreCase("reopen") || cycleType.equalsIgnoreCase("reopened") ? "reopen" : "start " + cycleType.toLowerCase()) + " SLA: no previous SLA cycle exists for ticket " + ticket.getId()
                        )
                );

        // 2. Calculate new allocation: half of previous cycle's allocated minutes
        int newAllocatedMinutes = previousSla.getAllocatedMinutes() / 2;
        if (newAllocatedMinutes < 1) {
            newAllocatedMinutes = 1; // Minimum 1 minute
        }

        // 3. Determine cycle number
        int cycleNumber = previousSla.getCycleNumber() + 1;

        // 4. Determine start time
        Instant effectiveBaseInstant = baseInstant != null
                ? baseInstant
                : Instant.now();

        Instant startAt = workingCalendarService.moveToWorkingTime(effectiveBaseInstant, departmentId, departmentZone);

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

        // 8. Determine initial event
        SlaEventType nextEventType;
        Instant nextEventAt;

        if (warningAt != null && warningAt.isBefore(deadline)) {
            nextEventType = SlaEventType.WARNING;
            nextEventAt = warningAt;
        } else {
            nextEventType = SlaEventType.BREACH;
            nextEventAt = deadline;
        }

        // 9. Create new SLA instance
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
                .nextEventType(nextEventType)
                .nextEventAt(nextEventAt)
                .pausedAt(null)
                .breachedAt(null)
                .build();

        SlaInstance savedSla = slaInstanceRepository.save(slaInstance);
        slaDynamicScheduler.scheduleSlaEvent(savedSla.getId(), savedSla.getNextEventAt());

        log.info("{} SLA cycle {} started: ticketId={}, slaInstanceId={}, allocatedMinutes={}, deadline={}, nextEventType={}, nextEventAt={}",
                cycleType, cycleNumber, ticket.getId(), savedSla.getId(), newAllocatedMinutes, deadline, nextEventType, nextEventAt);

        return savedSla;
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
            slaInstance.setNextEventType(null);
            slaInstance.setNextEventAt(null);
            SlaInstance saved = slaInstanceRepository.save(slaInstance);
            log.info("SLA paused: ticketId={}, slaInstanceId={}", ticket.getId(), saved.getId());
            return saved;
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

            if (slaInstance.getWarningAt() != null) {
                if (now.isBefore(slaInstance.getWarningAt())) {
                    slaInstance.setStatus(SlaStatus.ACTIVE);
                    slaInstance.setNextEventType(SlaEventType.WARNING);
                    slaInstance.setNextEventAt(slaInstance.getWarningAt());
                } else {
                    slaInstance.setStatus(SlaStatus.WARNING);
                    slaInstance.setNextEventType(SlaEventType.BREACH);
                    slaInstance.setNextEventAt(slaInstance.getCurrentDeadlineAt());
                }
            } else {
                slaInstance.setStatus(SlaStatus.ACTIVE);
                slaInstance.setNextEventType(SlaEventType.BREACH);
                slaInstance.setNextEventAt(slaInstance.getCurrentDeadlineAt());
            }

            slaInstance.setPausedAt(null);
            SlaInstance savedSla = slaInstanceRepository.save(slaInstance);
            slaDynamicScheduler.scheduleSlaEvent(savedSla.getId(), savedSla.getNextEventAt());

            log.info("SLA resumed: ticketId={}, slaInstanceId={}, newDeadline={}, nextEventType={}, nextEventAt={}",
                    ticket.getId(), savedSla.getId(), savedSla.getCurrentDeadlineAt(), savedSla.getNextEventType(), savedSla.getNextEventAt());

            return savedSla;
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
            slaInstance.setNextEventType(null);
            slaInstance.setNextEventAt(null);
            SlaInstance saved = slaInstanceRepository.save(slaInstance);
            log.info("SLA completed: ticketId={}, slaInstanceId={}", ticket.getId(), saved.getId());
            return saved;
        }

        return slaInstance;
    }

    /**
     * Event-driven processing for an SLA instance.
     * Validates eligibility, current nextEventType, and updates atomic state.
     */
    @Override
    @Transactional
    public void processSlaEvent(Long slaInstanceId) {
        if (slaInstanceId == null) {
            return;
        }

        SlaInstance sla = slaInstanceRepository.findByIdWithLock(slaInstanceId).orElse(null);
        if (sla == null) {
            log.warn("SLA instance not found for processing: id={}", slaInstanceId);
            return;
        }

        // Validate ticket and SLA eligibility
        if (sla.getStatus() != SlaStatus.ACTIVE && sla.getStatus() != SlaStatus.WARNING) {
            log.debug("Ignoring SLA event for id={}: current status is {}", slaInstanceId, sla.getStatus());
            return;
        }

        SlaEventType eventType = sla.getNextEventType();
        Instant eventAt = sla.getNextEventAt();

        if (eventType == null || eventAt == null) {
            log.debug("Ignoring SLA event for id={}: no next event configured", slaInstanceId);
            return;
        }

        Instant now = Instant.now();

        // Stale event check: if the recorded nextEventAt is in the future, this execution was from an obsolete schedule
        if (eventAt.isAfter(now.plusSeconds(1))) {
            log.debug("Ignoring stale SLA event for id={}: eventAt {} is in the future", slaInstanceId, eventAt);
            return;
        }

        if (eventType == SlaEventType.WARNING) {
            if (sla.getStatus() != SlaStatus.ACTIVE) {
                log.debug("Ignoring WARNING event for SLA id={}: status is {}", slaInstanceId, sla.getStatus());
                return;
            }

            log.info("Processing SLA WARNING event for id={}", slaInstanceId);
            sendWarningNotifications(sla);

            sla.setStatus(SlaStatus.WARNING);
            sla.setNextEventType(SlaEventType.BREACH);
            sla.setNextEventAt(sla.getCurrentDeadlineAt());

            SlaInstance updatedSla = slaInstanceRepository.save(sla);
            log.info("SLA warning processed for id={}, next event scheduled for BREACH at {}",
                    updatedSla.getId(), updatedSla.getNextEventAt());

            slaDynamicScheduler.scheduleSlaEvent(updatedSla.getId(), updatedSla.getNextEventAt());

        } else if (eventType == SlaEventType.BREACH) {
            log.info("Processing SLA BREACH event for id={}", slaInstanceId);
            sendBreachNotifications(sla);

            sla.setStatus(SlaStatus.BREACHED);
            sla.setBreachedAt(now);
            sla.setNextEventType(null);
            sla.setNextEventAt(null);

            slaInstanceRepository.save(sla);
            log.info("SLA breached successfully for id={}", slaInstanceId);
        }
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
    public SlaPolicyResponseDTO getSlaPolicyById(Long id) {
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

    private SlaPolicyResponseDTO toResponse(SlaPolicy policy) {
        return SlaPolicyResponseDTO.builder()
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

        return Boolean.TRUE.equals(
                departmentManagerRepository
                        .existsByEmployeeIdAndDepartmentId(
                                employeeId,
                                departmentId
                        )
        );
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