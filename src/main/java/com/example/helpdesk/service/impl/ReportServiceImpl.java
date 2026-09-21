package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.response.*;
import com.example.helpdesk.entity.*;
import com.example.helpdesk.enums.Priority;
import com.example.helpdesk.enums.SlaStatus;
import com.example.helpdesk.enums.TicketStatus;
import com.example.helpdesk.repository.*;
import com.example.helpdesk.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final TicketRepository ticketRepository;
    private final TicketSlaRepository ticketSlaRepository;
    private final TicketFeedbackRepository ticketFeedbackRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final DepartmentAgentRepository departmentAgentRepository;

    @Override
    public ReportSummaryResponse getDashboardSummary() {
        long totalTickets = ticketRepository.count();
        long openTickets = ticketRepository.findByStatus(TicketStatus.OPEN.name()).size()
                + ticketRepository.findByStatus(TicketStatus.ASSIGNED.name()).size()
                + ticketRepository.findByStatus(TicketStatus.IN_PROGRESS.name()).size();
        long resolvedTickets = ticketRepository.findByStatus(TicketStatus.RESOLVED.name()).size();
        long breachedTickets = ticketRepository.findByStatus(TicketStatus.BREACHED.name()).size();
        long reopenedTickets = ticketRepository.findAll().stream()
                .filter(t -> t.getReopenCount() > 0)
                .count();

        List<TicketSla> allSlas = ticketSlaRepository.findAll();
        long slaMetCount = allSlas.stream()
                .filter(sla -> SlaStatus.COMPLETED.name().equals(sla.getStatus()))
                .count();
        long slaBreachedCount = allSlas.stream()
                .filter(sla -> SlaStatus.BREACHED.name().equals(sla.getStatus()))
                .count();
        long slaWarningCount = allSlas.stream()
                .filter(sla -> SlaStatus.WARNING.name().equals(sla.getStatus()))
                .count();

        double averageResolutionTime = calculateAverageResolutionTime();
        double averageSlaResolutionTime = calculateAverageSlaResolutionTime();

        List<TicketFeedback> allFeedback = ticketFeedbackRepository.findAll();
        double averageFeedbackRating = allFeedback.stream()
                .mapToInt(TicketFeedback::getRating)
                .average()
                .orElse(0.0);

        return ReportSummaryResponse.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .resolvedTickets(resolvedTickets)
                .breachedTickets(breachedTickets)
                .reopenedTickets(reopenedTickets)
                .slaMetCount(slaMetCount)
                .slaBreachedCount(slaBreachedCount)
                .slaWarningCount(slaWarningCount)
                .averageResolutionTime(averageResolutionTime)
                .averageSlaResolutionTime(averageSlaResolutionTime)
                .averageFeedbackRating(averageFeedbackRating)
                .totalFeedbackCount((long) allFeedback.size())
                .build();
    }

    @Override
    public List<TicketCountByStatusResponse> getTicketCountByStatus() {
        return List.of(
                TicketStatus.values()
        ).stream()
                .map(status -> {
                    long count = ticketRepository.findByStatus(status.name()).size();
                    return TicketCountByStatusResponse.builder()
                            .status(status.name())
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketCountByPriorityResponse> getTicketCountByPriority() {
        return List.of(
                Priority.values()
        ).stream()
                .map(priority -> {
                    long count = ticketRepository.findAll().stream()
                            .filter(t -> priority.name().equals(t.getPriority()))
                            .count();
                    return TicketCountByPriorityResponse.builder()
                            .priority(priority.name())
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketCountByDepartmentResponse> getTicketCountByDepartment() {
        return departmentRepository.findAll().stream()
                .map(dept -> {
                    long count = ticketRepository.findByDepartmentId(dept.getId()).size();
                    return TicketCountByDepartmentResponse.builder()
                            .departmentId(dept.getId())
                            .departmentName(dept.getName())
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketCountByCategoryResponse> getTicketCountByCategory() {
        return categoryRepository.findAll().stream()
                .map(category -> {
                    long count = ticketRepository.findAll().stream()
                            .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(category.getId()))
                            .count();
                    return TicketCountByCategoryResponse.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketCountByAgentResponse> getTicketCountByAgent() {
        return departmentAgentRepository.findAll().stream()
                .map(agent -> {
                    long count = ticketRepository.findByAssignedAgentId(agent.getId()).size();
                    return TicketCountByAgentResponse.builder()
                            .agentId(agent.getId())
                            .agentName(agent.getEmployee().getFirstName() + " " + agent.getEmployee().getLastName())
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketCountByCategoryResponse> getTicketCountBySubCategory() {
        return subCategoryRepository.findAll().stream()
                .map(subCategory -> {
                    long count = ticketRepository.findAll().stream()
                            .filter(t -> t.getSubCategory() != null && t.getSubCategory().getId().equals(subCategory.getId()))
                            .count();
                    return TicketCountByCategoryResponse.builder()
                            .categoryId(subCategory.getId())
                            .categoryName(subCategory.getName())
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public FeedbackSummaryResponse getFeedbackSummary() {
        List<TicketFeedback> allFeedback = ticketFeedbackRepository.findAll();

        double averageRating = allFeedback.stream()
                .mapToInt(TicketFeedback::getRating)
                .average()
                .orElse(0.0);

        Map<Integer, Long> ratingDistribution = allFeedback.stream()
                .collect(Collectors.groupingBy(TicketFeedback::getRating, Collectors.counting()));

        return FeedbackSummaryResponse.builder()
                .totalFeedbackCount((long) allFeedback.size())
                .averageRating(averageRating)
                .ratingDistribution(ratingDistribution)
                .build();
    }

    @Override
    public List<WorkloadResponse> getAgentWorkload() {
        return departmentAgentRepository.findAll().stream()
                .map(agent -> {
                    long activeTickets = ticketRepository.findByAssignedAgentId(agent.getId()).stream()
                            .filter(t -> !t.getStatus().equals(TicketStatus.RESOLVED.name())
                                    && !t.getStatus().equals(TicketStatus.CLOSED.name())
                                    && !t.getStatus().equals(TicketStatus.CANCELLED.name()))
                            .count();
                    long resolvedTickets = ticketRepository.findByAssignedAgentId(agent.getId()).stream()
                            .filter(t -> t.getStatus().equals(TicketStatus.RESOLVED.name()))
                            .count();
                    return WorkloadResponse.builder()
                            .agentId(agent.getId())
                            .agentName(agent.getEmployee().getFirstName() + " " + agent.getEmployee().getLastName())
                            .departmentId(agent.getDepartment().getId())
                            .departmentName(agent.getDepartment().getName())
                            .activeTickets(activeTickets)
                            .resolvedTickets(resolvedTickets)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkloadResponse> getDepartmentWorkload() {
        return departmentRepository.findAll().stream()
                .map(dept -> {
                    long activeTickets = ticketRepository.findByDepartmentId(dept.getId()).stream()
                            .filter(t -> !t.getStatus().equals(TicketStatus.RESOLVED.name())
                                    && !t.getStatus().equals(TicketStatus.CLOSED.name())
                                    && !t.getStatus().equals(TicketStatus.CANCELLED.name()))
                            .count();
                    long resolvedTickets = ticketRepository.findByDepartmentId(dept.getId()).stream()
                            .filter(t -> t.getStatus().equals(TicketStatus.RESOLVED.name()))
                            .count();
                    return WorkloadResponse.builder()
                            .agentId(dept.getId())
                            .agentName(dept.getName())
                            .departmentId(dept.getId())
                            .departmentName(dept.getName())
                            .activeTickets(activeTickets)
                            .resolvedTickets(resolvedTickets)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketCountByStatusResponse> getTicketVolumeByDate(LocalDateTime fromDate, LocalDateTime toDate) {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(fromDate) && t.getCreatedAt().isBefore(toDate))
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> TicketCountByStatusResponse.builder()
                        .status(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private double calculateAverageResolutionTime() {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getResolvedAt() != null && t.getCreatedAt() != null)
                .mapToLong(t -> ChronoUnit.MINUTES.between(t.getCreatedAt(), t.getResolvedAt()))
                .average()
                .orElse(0.0);
    }

    private double calculateAverageSlaResolutionTime() {
        return ticketSlaRepository.findAll().stream()
                .filter(sla -> SlaStatus.COMPLETED.name().equals(sla.getStatus())
                        && sla.getSlaStartAt() != null
                        && sla.getTicket().getResolvedAt() != null)
                .mapToLong(sla -> ChronoUnit.MINUTES.between(sla.getSlaStartAt(), sla.getTicket().getResolvedAt()))
                .average()
                .orElse(0.0);
    }
}
