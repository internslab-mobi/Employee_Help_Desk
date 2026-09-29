package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hd_sla_policies")
public class HDSlaPolicy extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "department_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sla_policy_department")
    )
    private HDDepartment department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sub_category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sla_policy_sub_category")
    )
    private HDSubCategory subCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private TicketPriority priority;

    @Column(name = "resolution_time_minutes", nullable = false)
    private Integer resolutionTimeMinutes;

    @Column(name = "warning_time_minutes", nullable = false)
    private Integer warningTimeMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "calendar_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sla_policy_calendar")
    )
    private HDBusinessCalendar calendar;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}