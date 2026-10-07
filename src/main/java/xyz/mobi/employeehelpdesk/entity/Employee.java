package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;
import xyz.mobi.employeehelpdesk.entity.enums.UserRole;

import java.time.LocalDate;

@Entity
@Table(
        name = "hd_employees",
        indexes = {
                @Index(name = "idx_employee_department", columnList = "department_id"),
                @Index(name = "idx_employee_status", columnList = "employmentStatus"),
                @Index(name = "idx_employee_role", columnList = "role"),
                @Index(
                        name = "idx_employee_dept_status_role",
                        columnList = "department_id, employmentStatus, role"
                )
        }
)
@Getter
@Setter
public class Employee extends BaseEntity {

    @Column(unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 30, unique = true)
    private String phone;

    @Column(length = 100)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentStatus employmentStatus;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30)
    private UserRole role;

    private LocalDate dateOfJoining;

    private LocalDate dateOfExit;

    @Column(nullable = false, length = 50)
    private String timezone = "UTC";

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private DepartmentManager manager;
}