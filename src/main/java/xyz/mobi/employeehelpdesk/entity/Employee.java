package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.mobi.employeehelpdesk.entity.enums.EmploymentStatus;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 30)
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
    private xyz.mobi.employeehelpdesk.entity.enums.UserRole role;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    private LocalDate dateOfJoining;

    private LocalDate dateOfExit;

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