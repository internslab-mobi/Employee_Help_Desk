package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "hd_employee_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_skill",
                        columnNames = {"employee_id", "skill_id"}
                )
        }
)
public class HDEmployeeSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_skill_employee")
    )
    private HDEmployee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_skill_skill")
    )
    private HDSkill skill;
}