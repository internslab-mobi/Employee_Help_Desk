package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hd_sub_categories",
        uniqueConstraints = {@UniqueConstraint(
                        name = "uk_category_sub_category_name",
                        columnNames = {"category_id", "name"}
                )
        }
)
public class HDSubCategoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private HDCategoryEntity category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean active;
}