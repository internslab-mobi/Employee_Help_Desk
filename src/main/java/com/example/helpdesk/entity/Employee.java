package com.example.helpdesk.entity;

import com.example.helpdesk.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "hd_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
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

    @Column(name = "employment_status", nullable = false, length = 30)
    private String employmentStatus;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "date_of_exit")
    private LocalDate dateOfExit;

    @Column(name = "profile_image", columnDefinition = "longblob")
    private byte[] profileImage;

    @Column(name = "profile_image_type", length = 100)
    private String profileImageType;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private Role role;

    @Column(length = 50)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}