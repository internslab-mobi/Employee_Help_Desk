package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "sla_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sla_policy_department_subcategory",
                        columnNames = {"department_id", "sub_category_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_sla_policy_department",
                        columnList = "department_id"
                ),
                @Index(
                        name = "idx_sla_policy_subcategory",
                        columnList = "sub_category_id"
                )
        }
)
@Getter
@Setter
public class SlaPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    @Column(nullable = false)
    private Integer durationMinutes;

    private Integer warningMinutes;

    @Column(nullable = false)
    private Boolean isActive = true;
}