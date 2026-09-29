package com.divya.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hd_error_codes")
public class HDErrorCode extends BaseEntity {

    @Column(name = "error_code", nullable = false, unique = true, length = 20)
    private String errorCode;

    @Column(name = "exception_type", nullable = false, unique = true, length = 100)
    private String exceptionType;
}