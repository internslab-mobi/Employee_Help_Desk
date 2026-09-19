package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hd_department_managers",
        uniqueConstraints = {@UniqueConstraint(name = "uk_dept_manager", columnNames = {"department_id", "employee_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDDepartmentManager extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private HDDepartment department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private HDEmployee employee;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
}
