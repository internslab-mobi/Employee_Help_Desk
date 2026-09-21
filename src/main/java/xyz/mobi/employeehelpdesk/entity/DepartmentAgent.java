package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "department_agents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_agent",
                        columnNames = {"department_id", "employee_id"}
                )
        }
)
@Getter
@Setter
public class DepartmentAgent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 30)
//    private DepartmentAgentStatus status;

    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;

    /*@Column(nullable = false)
    private Boolean availableForAssignment = true;*/

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(
            name = "ticket_status_counts",
            columnDefinition = "JSON",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private String ticketStatusCounts;
}