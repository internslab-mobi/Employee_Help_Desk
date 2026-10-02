package com.divya.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "hd_sub_category_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sub_category_skill",
                        columnNames = {"sub_category_id", "skill_id"}
                )
        }
)
public class HDSubCategorySkillEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sub_category_skill_sub_category")
    )
    private HDSubCategoryEntity subCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sub_category_skill_skill")
    )
    private HDSkillEntity skill;
}