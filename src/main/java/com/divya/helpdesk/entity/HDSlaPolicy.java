package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hd_sla_policies",
        uniqueConstraints = {@UniqueConstraint(name = "uk_dept_subcat_sla", columnNames = {"department_id", "sub_category_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDSlaPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private HDDepartment department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private HDSubCategory subCategory;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "warning_minutes", nullable = false)
    private Integer warningMinutes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
