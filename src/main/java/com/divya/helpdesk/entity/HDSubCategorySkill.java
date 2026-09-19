package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hd_sub_category_skills",
        uniqueConstraints = {@UniqueConstraint(name = "uk_subcat_skill", columnNames = {"sub_category_id", "skill_id"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HDSubCategorySkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private HDSubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private HDSkill skill;
}
