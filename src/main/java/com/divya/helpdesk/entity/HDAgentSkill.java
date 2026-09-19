package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hd_agent_skills",
        uniqueConstraints = {@UniqueConstraint(name = "uk_agent_skill", columnNames = {"agent_id", "skill_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDAgentSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private HDDepartmentAgent agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private HDSkill skill;
}
