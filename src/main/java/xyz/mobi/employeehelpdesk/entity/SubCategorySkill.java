package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "sub_category_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sub_category_skill",
                        columnNames = {"sub_category_id", "skill_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SubCategorySkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}