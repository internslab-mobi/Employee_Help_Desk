package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hd_categories",
        uniqueConstraints = {@UniqueConstraint(
                        name = "uk_department_category_name",
                        columnNames = {"department_id", "name"}
                )
        }
)
public class HDCategoryEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private HDDepartmentEntity department;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}