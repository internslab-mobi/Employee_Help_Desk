package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hd_department_agents",
        uniqueConstraints = {@UniqueConstraint(name = "uk_dept_agent", columnNames = {"department_id", "employee_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDDepartmentAgent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private HDDepartment department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private HDEmployee employee;

    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;
}
