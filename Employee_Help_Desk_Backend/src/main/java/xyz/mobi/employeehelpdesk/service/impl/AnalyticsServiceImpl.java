package xyz.mobi.employeehelpdesk.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.mobi.employeehelpdesk.dto.analytics.*;
import xyz.mobi.employeehelpdesk.entity.DepartmentAgent;
import xyz.mobi.employeehelpdesk.entity.Employee;
import xyz.mobi.employeehelpdesk.entity.SlaInstance;
import xyz.mobi.employeehelpdesk.entity.Ticket;
import xyz.mobi.employeehelpdesk.entity.enums.*;
import xyz.mobi.employeehelpdesk.exception.ResourceNotFoundException;
import xyz.mobi.employeehelpdesk.repository.*;
import xyz.mobi.employeehelpdesk.service.AnalyticsService;
import xyz.mobi.employeehelpdesk.service.AuthService;
import xyz.mobi.employeehelpdesk.util.InstantDateRange;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AuthService authService;
    private final EmployeeRepository employeeRepository;
    private final TicketRepository ticketRepository;
    private final DepartmentAgentRepository departmentAgentRepository;
    private final SlaInstanceRepository slaInstanceRepository;
    private final DepartmentRepository departmentRepository;

    private record AuthorizedScope(
            Employee employee,
            UserRole role,
            Long requesterId,
            Long agentId,
            Long departmentId
    ) {}

    private AuthorizedScope resolveScope(Long requestedDepartmentId) {
        Long currentEmployeeId = authService.getCurrentEmployeeId();
        Employee employee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + currentEmployeeId));

        UserRole role = employee.getRole() != null ? employee.getRole() : UserRole.EMPLOYEE;

        Long requesterId = null;
        Long agentId = null;
        Long effectiveDepartmentId = null;

        switch (role) {
            case EMPLOYEE -> {
                requesterId = employee.getId();
            }
            case AGENT -> {
                Optional<DepartmentAgent> agentOpt = departmentAgentRepository.findByEmployeeId(employee.getId());
                if (agentOpt.isPresent()) {
                    agentId = agentOpt.get().getId();
                } else {
                    requesterId = employee.getId();
                }
            }
            case MANAGER -> {
                if (employee.getDepartment() != null) {
                    effectiveDepartmentId = employee.getDepartment().getId();
                }
            }
            case ADMIN -> {
                effectiveDepartmentId = requestedDepartmentId;
            }
        }

        return new AuthorizedScope(employee, role, requesterId, agentId, effectiveDepartmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardAnalyticsResponseDTO getDashboardAnalytics(LocalDate fromDate, LocalDate toDate, Long departmentId) {
        AuthorizedScope scope = resolveScope(departmentId);
        Employee employee = scope.employee();
        UserRole role = scope.role();

        InstantDateRange dateRange = InstantDateRange.of(fromDate, toDate, employee.getTimezone());
        Instant from = dateRange.from();
        Instant to = dateRange.toExclusive();

        Long requesterId = scope.requesterId();
        Long agentId = scope.agentId();
        Long effectiveDeptId = scope.departmentId();

        // 1. Status Breakdown
        List<Object[]> statusRows = ticketRepository.countScopedTicketsByStatus(
                requesterId, agentId, effectiveDeptId, from, to
        );
        Map<TicketStatus, Long> statusMap = new EnumMap<>(TicketStatus.class);
        for (TicketStatus ts : TicketStatus.values()) {
            statusMap.put(ts, 0L);
        }
        long totalTickets = 0;
        for (Object[] row : statusRows) {
            TicketStatus status = (TicketStatus) row[0];
            Long count = (Long) row[1];
            if (status != null && count != null) {
                statusMap.put(status, count);
                totalTickets += count;
            }
        }

        long openTickets = statusMap.getOrDefault(TicketStatus.OPEN, 0L);
        long inProgressTickets = statusMap.getOrDefault(TicketStatus.IN_PROGRESS, 0L);
        long onHoldTickets = statusMap.getOrDefault(TicketStatus.ON_HOLD, 0L);
        long resolvedTickets = statusMap.getOrDefault(TicketStatus.RESOLVED, 0L);
        long withdrawnTickets = statusMap.getOrDefault(TicketStatus.WITHDRAWN, 0L);
        long reopenedTickets = statusMap.getOrDefault(TicketStatus.REOPENED, 0L);

        List<DistributionItemDTO> statusDistribution = new ArrayList<>();
        for (TicketStatus ts : TicketStatus.values()) {
            long count = statusMap.getOrDefault(ts, 0L);
            double pct = totalTickets > 0 ? Math.round((count * 100.0 / totalTickets) * 10.0) / 10.0 : 0.0;
            statusDistribution.add(new DistributionItemDTO(ts.name(), formatLabel(ts.name()), count, pct));
        }

        // 2. Priority Breakdown
        List<Object[]> priorityRows = ticketRepository.countScopedTicketsByPriority(
                requesterId, agentId, effectiveDeptId, from, to
        );
        Map<Priority, Long> priorityMap = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            priorityMap.put(p, 0L);
        }
        for (Object[] row : priorityRows) {
            Priority priority = (Priority) row[0];
            Long count = (Long) row[1];
            if (priority != null && count != null) {
                priorityMap.put(priority, count);
            }
        }
        List<DistributionItemDTO> priorityDistribution = new ArrayList<>();
        for (Priority p : Priority.values()) {
            long count = priorityMap.getOrDefault(p, 0L);
            double pct = totalTickets > 0 ? Math.round((count * 100.0 / totalTickets) * 10.0) / 10.0 : 0.0;
            priorityDistribution.add(new DistributionItemDTO(p.name(), formatLabel(p.name()), count, pct));
        }

        // 3. SLA Stats
        long slaBreached = ticketRepository.countTicketsByLatestSlaStatus(
                requesterId, agentId, effectiveDeptId, from, to, SlaStatus.BREACHED
        );
        long slaWarning = ticketRepository.countTicketsByLatestSlaStatus(
                requesterId, agentId, effectiveDeptId, from, to, SlaStatus.WARNING
        );
        long slaCompliant = Math.max(0, totalTickets - slaBreached);
        double slaComplianceRate = totalTickets > 0
                ? Math.round((slaCompliant * 100.0 / totalTickets) * 10.0) / 10.0
                : 100.0;

        // 4. Resolution time average (in hours)
        List<Object[]> intervals = ticketRepository.findResolutionIntervals(
                requesterId, agentId, effectiveDeptId, from, to
        );
        double totalResolutionSeconds = 0;
        int resolvedCount = 0;
        for (Object[] row : intervals) {
            Instant cr = (Instant) row[0];
            Instant res = (Instant) row[1];
            if (cr != null && res != null && !res.isBefore(cr)) {
                totalResolutionSeconds += Duration.between(cr, res).getSeconds();
                resolvedCount++;
            }
        }
        double avgResolutionHours = resolvedCount > 0
                ? Math.round((totalResolutionSeconds / resolvedCount / 3600.0) * 10.0) / 10.0
                : 0.0;

        // 5. Assigned & Unassigned count
        long assignedCount = 0;
        long unassignedCount = 0;
        if (role == UserRole.ADMIN || role == UserRole.MANAGER) {
            List<Object[]> assignRows = ticketRepository.countAssignedAndUnassignedTickets(effectiveDeptId, from, to);
            if (!assignRows.isEmpty()) {
                Object[] r = assignRows.get(0);
                assignedCount = r[0] != null ? ((Number) r[0]).longValue() : 0;
                unassignedCount = r[1] != null ? ((Number) r[1]).longValue() : 0;
            }
        } else if (role == UserRole.AGENT) {
            assignedCount = totalTickets;
            unassignedCount = 0;
        } else if (role == UserRole.EMPLOYEE && requesterId != null) {
            assignedCount = ticketRepository.countAssignedTicketsForRequester(requesterId, from, to);
            unassignedCount = Math.max(0, totalTickets - assignedCount);
        }

        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .inProgressTickets(inProgressTickets)
                .onHoldTickets(onHoldTickets)
                .resolvedTickets(resolvedTickets)
                .withdrawnTickets(withdrawnTickets)
                .reopenedTickets(reopenedTickets)
                .overdueTickets(slaBreached)
                .slaBreachedTickets(slaBreached)
                .slaWarningTickets(slaWarning)
                .slaCompliantTickets(slaCompliant)
                .slaComplianceRate(slaComplianceRate)
                .avgResolutionHours(avgResolutionHours)
                .assignedTickets(assignedCount)
                .unassignedTickets(unassignedCount)
                .build();

        // 6. Trends over time
        List<TrendItemDTO> trends = calculateTrends(requesterId, agentId, effectiveDeptId, from, to, employee.getTimezone());

        // 7. Department Distribution (Admin / Manager)
        List<DepartmentMetricDTO> departmentDistribution = new ArrayList<>();
        if (role == UserRole.ADMIN) {
            List<Object[]> deptRows = ticketRepository.countTicketsByDepartment(from, to);
            for (Object[] row : deptRows) {
                Long dId = (Long) row[0];
                String dName = (String) row[1];
                long tCount = row[2] != null ? ((Number) row[2]).longValue() : 0;
                long oCount = row[3] != null ? ((Number) row[3]).longValue() : 0;
                long rCount = row[4] != null ? ((Number) row[4]).longValue() : 0;
                departmentDistribution.add(new DepartmentMetricDTO(dId, dName, tCount, oCount, rCount));
            }
        } else if (role == UserRole.MANAGER && employee.getDepartment() != null) {
            departmentDistribution.add(new DepartmentMetricDTO(
                    employee.getDepartment().getId(),
                    employee.getDepartment().getName(),
                    totalTickets,
                    openTickets + inProgressTickets + onHoldTickets + reopenedTickets,
                    resolvedTickets
            ));
        }

        // 8. Category Distribution (Admin / Manager)
        List<DistributionItemDTO> categoryDistribution = new ArrayList<>();
        if (role == UserRole.ADMIN || role == UserRole.MANAGER) {
            List<Object[]> catRows = ticketRepository.countTicketsByCategory(effectiveDeptId, from, to);
            for (Object[] row : catRows) {
                String cName = (String) row[0];
                long cCount = row[1] != null ? ((Number) row[1]).longValue() : 0;
                double pct = totalTickets > 0 ? Math.round((cCount * 100.0 / totalTickets) * 10.0) / 10.0 : 0.0;
                categoryDistribution.add(new DistributionItemDTO(cName, cName, cCount, pct));
            }
        }

        // 9. Agent Workload (Admin / Manager)
        List<AgentWorkloadDTO> agentWorkload = new ArrayList<>();
        if (role == UserRole.ADMIN || role == UserRole.MANAGER) {
            List<DepartmentAgent> agents = (role == UserRole.MANAGER && effectiveDeptId != null)
                    ? departmentAgentRepository.findByDepartmentId(effectiveDeptId)
                    : departmentAgentRepository.findAll();

            if (!agents.isEmpty()) {
                List<Long> agentIds = agents.stream().map(DepartmentAgent::getId).toList();
                List<Object[]> activeRows = ticketRepository.countActiveTicketsForAgents(
                        agentIds,
                        List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD, TicketStatus.REOPENED)
                );
                Map<Long, Long> activeMap = new HashMap<>();
                for (Object[] row : activeRows) {
                    Long aId = (Long) row[0];
                    Long cnt = (Long) row[1];
                    if (aId != null && cnt != null) {
                        activeMap.put(aId, cnt);
                    }
                }

                for (DepartmentAgent da : agents) {
                    Employee ae = da.getEmployee();
                    long activeCount = activeMap.getOrDefault(da.getId(), 0L);
                    agentWorkload.add(AgentWorkloadDTO.builder()
                            .agentId(da.getId())
                            .employeeId(ae != null ? ae.getId() : null)
                            .agentName(ae != null ? (ae.getFirstName() + " " + (ae.getLastName() != null ? ae.getLastName() : "")).trim() : "Unknown Agent")
                            .email(ae != null ? ae.getEmail() : "")
                            .departmentName(ae != null && ae.getDepartment() != null ? ae.getDepartment().getName() : "")
                            .activeTickets(activeCount)
                            .resolvedTickets(0) // maintained in workload summary
                            .totalTickets(activeCount)
                            .build());
                }
            }
        }

        // 10. Employee Statistics (Admin only)
        EmployeeStatsDTO employeeStats = null;
        if (role == UserRole.ADMIN) {
            List<Object[]> empRows = employeeRepository.countEmployeesByStatus();
            long totalEmp = 0;
            long activeEmp = 0;
            long inactiveEmp = 0;
            long onLeaveEmp = 0;
            for (Object[] row : empRows) {
                EmploymentStatus es = (EmploymentStatus) row[0];
                Long count = (Long) row[1];
                if (es != null && count != null) {
                    totalEmp += count;
                    if (es == EmploymentStatus.ACTIVE) activeEmp = count;
                    else if (es == EmploymentStatus.INACTIVE) inactiveEmp = count;
                    else if (es == EmploymentStatus.ON_LEAVE) onLeaveEmp = count;
                }
            }
            employeeStats = new EmployeeStatsDTO(totalEmp, activeEmp, inactiveEmp, onLeaveEmp);
        }

        // 11. Recent Tickets
        List<Ticket> recentList = ticketRepository.findScopedRecentTickets(
                requesterId, agentId, effectiveDeptId, from, to, PageRequest.of(0, 10)
        );
        List<RecentTicketDTO> recentTickets = recentList.stream().map(this::mapToRecentTicketDTO).toList();

        return DashboardAnalyticsResponseDTO.builder()
                .role(role)
                .employeeId(employee.getId())
                .employeeName((employee.getFirstName() + " " + (employee.getLastName() != null ? employee.getLastName() : "")).trim())
                .employeeEmail(employee.getEmail())
                .designation(employee.getDesignation() != null ? employee.getDesignation() : "")
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : "")
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .summary(summary)
                .statusDistribution(statusDistribution)
                .priorityDistribution(priorityDistribution)
                .departmentDistribution(departmentDistribution)
                .categoryDistribution(categoryDistribution)
                .trends(trends)
                .agentWorkload(agentWorkload)
                .employeeStats(employeeStats)
                .recentTickets(recentTickets)
                .generatedAt(Instant.now())
                .fromDate(fromDate != null ? fromDate.toString() : null)
                .toDate(toDate != null ? toDate.toString() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportTicketDetailDTO> getExportTicketDetails(LocalDate fromDate, LocalDate toDate, Long departmentId) {
        AuthorizedScope scope = resolveScope(departmentId);
        Employee employee = scope.employee();

        InstantDateRange dateRange = InstantDateRange.of(fromDate, toDate, employee.getTimezone());
        Instant from = dateRange.from();
        Instant to = dateRange.toExclusive();

        List<Ticket> tickets = ticketRepository.findScopedRecentTickets(
                scope.requesterId(), scope.agentId(), scope.departmentId(), from, to, PageRequest.of(0, 1000)
        );

        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();
        Map<Long, SlaInstance> slaMap = new HashMap<>();
        if (!ticketIds.isEmpty()) {
            List<SlaInstance> slaList = slaInstanceRepository.findLatestByTicketIds(ticketIds);
            for (SlaInstance s : slaList) {
                if (s.getTicket() != null) {
                    slaMap.put(s.getTicket().getId(), s);
                }
            }
        }

        return tickets.stream().map(t -> {
            SlaInstance sla = slaMap.get(t.getId());
            String slaStatusStr = sla != null && sla.getStatus() != null ? sla.getStatus().name() : "N/A";
            Instant deadline = sla != null ? sla.getCurrentDeadlineAt() : null;

            String reqName = t.getRequester() != null
                    ? (t.getRequester().getFirstName() + " " + (t.getRequester().getLastName() != null ? t.getRequester().getLastName() : "")).trim()
                    : "Unknown";
            String reqEmail = t.getRequester() != null ? t.getRequester().getEmail() : "";

            String agentName = (t.getAssignedAgent() != null && t.getAssignedAgent().getEmployee() != null)
                    ? (t.getAssignedAgent().getEmployee().getFirstName() + " " +
                    (t.getAssignedAgent().getEmployee().getLastName() != null ? t.getAssignedAgent().getEmployee().getLastName() : "")).trim()
                    : "Unassigned";

            return ExportTicketDetailDTO.builder()
                    .id(t.getId())
                    .ticketNumber(t.getTicketNumber())
                    .subject(t.getSubject())
                    .departmentName(t.getDepartment() != null ? t.getDepartment().getName() : "N/A")
                    .categoryName(t.getCategory() != null ? t.getCategory().getName() : "N/A")
                    .subCategoryName(t.getSubCategory() != null ? t.getSubCategory().getName() : "N/A")
                    .status(t.getStatus())
                    .priority(t.getPriority())
                    .requesterName(reqName)
                    .requesterEmail(reqEmail)
                    .assignedAgentName(agentName)
                    .slaStatus(slaStatusStr)
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .resolvedAt(t.getResolvedAt())
                    .deadlineAt(deadline)
                    .build();
        }).toList();
    }

    private RecentTicketDTO mapToRecentTicketDTO(Ticket t) {
        String reqName = t.getRequester() != null
                ? (t.getRequester().getFirstName() + " " + (t.getRequester().getLastName() != null ? t.getRequester().getLastName() : "")).trim()
                : "Unknown";
        String agentName = (t.getAssignedAgent() != null && t.getAssignedAgent().getEmployee() != null)
                ? (t.getAssignedAgent().getEmployee().getFirstName() + " " +
                (t.getAssignedAgent().getEmployee().getLastName() != null ? t.getAssignedAgent().getEmployee().getLastName() : "")).trim()
                : "Unassigned";

        return RecentTicketDTO.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .subject(t.getSubject())
                .categoryName(t.getCategory() != null ? t.getCategory().getName() : "N/A")
                .subCategoryName(t.getSubCategory() != null ? t.getSubCategory().getName() : "N/A")
                .status(t.getStatus())
                .priority(t.getPriority())
                .slaStatus("ACTIVE")
                .requesterName(reqName)
                .assignedAgentName(agentName)
                .departmentName(t.getDepartment() != null ? t.getDepartment().getName() : "N/A")
                .createdAt(t.getCreatedAt())
                .resolvedAt(t.getResolvedAt())
                .build();
    }

    private List<TrendItemDTO> calculateTrends(
            Long requesterId, Long agentId, Long departmentId,
            Instant from, Instant to, String timezone
    ) {
        List<Object[]> rows = ticketRepository.findTimestampsForTrend(requesterId, agentId, departmentId, from, to);
        ZoneId zone = ZoneId.of(timezone != null && !timezone.isBlank() ? timezone.trim() : "UTC");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, long[]> bucketMap = new TreeMap<>();
        for (Object[] r : rows) {
            Instant cr = (Instant) r[0];
            Instant res = (Instant) r[1];

            if (cr != null) {
                String crDay = cr.atZone(zone).format(formatter);
                bucketMap.computeIfAbsent(crDay, k -> new long[]{0, 0})[0]++;
            }
            if (res != null) {
                String resDay = res.atZone(zone).format(formatter);
                bucketMap.computeIfAbsent(resDay, k -> new long[]{0, 0})[1]++;
            }
        }

        List<TrendItemDTO> trends = new ArrayList<>();
        bucketMap.forEach((day, counts) -> trends.add(new TrendItemDTO(day, counts[0], counts[1])));

        // If dataset is empty or small, provide default last 7 days buckets
        if (trends.isEmpty()) {
            LocalDate today = LocalDate.now(zone);
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                trends.add(new TrendItemDTO(d.format(formatter), 0, 0));
            }
        }

        return trends;
    }

    private String formatLabel(String enumName) {
        if (enumName == null) return "";
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
