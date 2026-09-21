package com.divya.helpdesk.entity;

import com.divya.helpdesk.enums.HDPriorityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hd_sub_categories",
        uniqueConstraints = {@UniqueConstraint(name = "uk_cat_subcat_name", columnNames = {"category_id", "name"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDSubCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private HDCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private HDEmployee createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HDPriorityLevel priority = HDPriorityLevel.MEDIUM;
}
