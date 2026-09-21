package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.mobi.employeehelpdesk.entity.enums.Priority;

@Entity
@Table(
        name = "sub_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sub_category_category_name",
                        columnNames = {"category_id", "name"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_sub_category_category",
                        columnList = "category_id"
                )
        }
)
@Getter
@Setter
public class SubCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Priority priority;
}