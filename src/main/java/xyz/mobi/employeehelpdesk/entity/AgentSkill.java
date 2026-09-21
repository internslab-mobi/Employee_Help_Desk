package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "agent_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agent_skill",
                        columnNames = {"agent_id", "skill_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_agent_skill_agent",
                        columnList = "agent_id"
                ),
                @Index(
                        name = "idx_agent_skill_skill",
                        columnList = "skill_id"
                )
        }
)
@Getter
@Setter
public class AgentSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private DepartmentAgent agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}