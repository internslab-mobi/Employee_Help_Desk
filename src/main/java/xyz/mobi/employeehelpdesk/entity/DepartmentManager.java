package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "department_managers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_manager",
                        columnNames = {"department_id", "employee_id"}
                )
        }
)
@Getter
@Setter
public class DepartmentManager extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Boolean isPrimary = false;
}