package xyz.mobi.employeehelpdesk.strategy.routing;

import org.springframework.stereotype.Component;
import xyz.mobi.employeehelpdesk.exception.BadRequestException;
import xyz.mobi.employeehelpdesk.entity.RoutingCandidate;

import java.util.Comparator;
import java.util.List;

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

        Comparator<RoutingCandidate> comparator =
                Comparator
                        // 1. Higher skill percentage first
                        .comparingDouble(
                                this::skillMatchPercentage
                        )
                        .reversed()

                        // 2. Lower active workload
                        .thenComparingLong(
                                RoutingCandidate::getActiveTicketCount
                        )

                        // 3. Longest time since assignment
                        // NULL means never assigned -> first
                        .thenComparing(
                                candidate ->
                                        candidate.getAgent()
                                                .getLastAssignedAt(),
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )

                        // 4. Lower agent ID
                        .thenComparing(
                                candidate ->
                                        candidate.getAgent().getId()
                        );

        return candidates.stream()
                .min(comparator)
                .orElseThrow(
                        () -> new BadRequestException(
                                "No routing candidate available"
                        )
                );
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