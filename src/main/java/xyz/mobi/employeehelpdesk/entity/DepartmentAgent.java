package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "hd_department_agents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_agent_employee",
                        columnNames = {"employee_id"}
                )
        }
)
@Getter
@Setter
public class DepartmentAgent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            unique = true
    )
    private Employee employee;

    @Column(name = "last_assigned_at")
    private Instant lastAssignedAt;

    /**
     * Maintained by MySQL triggers.
     * Hibernate only reads this field.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "ticket_status_counts",
            columnDefinition = "JSON",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private String ticketStatusCounts;
}