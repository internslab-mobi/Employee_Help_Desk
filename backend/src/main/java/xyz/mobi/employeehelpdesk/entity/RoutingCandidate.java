package xyz.mobi.employeehelpdesk.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoutingCandidate {

    private DepartmentAgent agent;

    private int skillMatchCount;

    private int requiredSkillCount;

    private long activeTicketCount;
}