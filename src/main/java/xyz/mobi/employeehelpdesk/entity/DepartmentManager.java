package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "department_managers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_manager_employee",
                        columnNames = {"employee_id"}
                )
        }
)
@Getter
@Setter
public class DepartmentManager extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            unique = true
    )
    private Employee employee;

    @Column(nullable = false)
    private Boolean isPrimary = false;
}