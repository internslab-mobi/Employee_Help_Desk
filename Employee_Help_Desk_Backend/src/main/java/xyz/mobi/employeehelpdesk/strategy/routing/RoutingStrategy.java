package xyz.mobi.employeehelpdesk.strategy.routing;

import xyz.mobi.employeehelpdesk.entity.RoutingCandidate;

import java.util.List;

public interface RoutingStrategy {

    RoutingCandidate selectCandidate(
            List<RoutingCandidate> candidates
    );
}