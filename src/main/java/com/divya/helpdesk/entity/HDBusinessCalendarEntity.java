package com.divya.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hd_business_calendars")
public class HDBusinessCalendarEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;
}