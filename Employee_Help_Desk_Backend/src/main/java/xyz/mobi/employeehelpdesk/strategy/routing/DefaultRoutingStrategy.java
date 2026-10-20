package xyz.mobi.employeehelpdesk.strategy.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.mobi.employeehelpdesk.entity.RoutingCandidate;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class DefaultRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingCandidate selectCandidate(
            List<RoutingCandidate> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            throw new BadRequestException(
                    "No eligible agents available for routing"
            );
        }

        boolean hasRequiredSkills = candidates.stream().anyMatch(c -> c.getRequiredSkillCount() > 0);

        RoutingCandidate selected;

        if (hasRequiredSkills) {
            List<RoutingCandidate> skilledCandidates = candidates.stream()
                    .filter(c -> c.getSkillMatchCount() > 0)
                    .toList();

            if (!skilledCandidates.isEmpty()) {
                // Hierarchy:
                // 3. Matching skills (higher first)
                // 4. Lowest workload
                // 5. Longest assigned time
                // 6. Lowest employee ID
                Comparator<RoutingCandidate> skilledComparator = Comparator
                        .comparingDouble(this::skillMatchPercentage)
                        .reversed()
                        .thenComparingLong(RoutingCandidate::getActiveTicketCount)
                        .thenComparing(
                                candidate -> candidate.getAgent().getLastAssignedAt(),
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        )
                        .thenComparingLong(this::getEmployeeId);

                selected = skilledCandidates.stream()
                        .min(skilledComparator)
                        .orElseThrow(() -> new BadRequestException("No skilled routing candidate available"));

                log.debug("Routing strategy selected skilled agentId={} employeeId={} (skillMatches={}/{}, activeWorkload={})",
                        selected.getAgent().getId(), getEmployeeId(selected), selected.getSkillMatchCount(), selected.getRequiredSkillCount(), selected.getActiveTicketCount());
            } else {
                // When no skilled agents found -> choose ACTIVE agents with lowest employee id (with workload & assigned time fallbacks)
                Comparator<RoutingCandidate> fallbackComparator = Comparator
                        .comparingLong(this::getEmployeeId)
                        .thenComparingLong(RoutingCandidate::getActiveTicketCount)
                        .thenComparing(
                                candidate -> candidate.getAgent().getLastAssignedAt(),
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        );

                selected = candidates.stream()
                        .min(fallbackComparator)
                        .orElseThrow(() -> new BadRequestException("No routing candidate available"));

                log.info("No skilled agents found; fallback selected active agentId={} employeeId={}",
                        selected.getAgent().getId(), getEmployeeId(selected));
            }
        } else {
            // Sub-category "others" or no required skills configured:
            // 4. Lowest workload
            // 5. Longest assigned time
            // 6. Lowest employee ID
            Comparator<RoutingCandidate> workloadComparator = Comparator
                    .comparingLong(RoutingCandidate::getActiveTicketCount)
                    .thenComparing(
                            candidate -> candidate.getAgent().getLastAssignedAt(),
                            Comparator.nullsFirst(Comparator.naturalOrder())
                    )
                    .thenComparingLong(this::getEmployeeId);

            selected = candidates.stream()
                    .min(workloadComparator)
                    .orElseThrow(() -> new BadRequestException("No routing candidate available"));

            log.debug("Routing strategy selected agentId={} employeeId={} by lowest workload (activeWorkload={})",
                    selected.getAgent().getId(), getEmployeeId(selected), selected.getActiveTicketCount());
        }

        return selected;
    }

    private long getEmployeeId(RoutingCandidate candidate) {
        if (candidate.getAgent() != null
                && candidate.getAgent().getEmployee() != null
                && candidate.getAgent().getEmployee().getId() != null) {
            return candidate.getAgent().getEmployee().getId();
        }
        return candidate.getAgent() != null && candidate.getAgent().getId() != null
                ? candidate.getAgent().getId()
                : Long.MAX_VALUE;
    }

    private double skillMatchPercentage(
            RoutingCandidate candidate) {

        if (candidate.getRequiredSkillCount() == 0) {
            return 0.0;
        }

        return (double) candidate.getSkillMatchCount()
                / candidate.getRequiredSkillCount();
    }
}