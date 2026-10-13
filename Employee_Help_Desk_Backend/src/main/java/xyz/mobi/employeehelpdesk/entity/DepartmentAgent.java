package xyz.mobi.employeehelpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "hd_department_agents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_agent_employee",
                        columnNames = {"employee_id"}
                )
        }
)
@Getter
@Setter
public class DepartmentAgent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            unique = true
    )
    private Employee employee;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hd_agent_categories",
            joinColumns = @JoinColumn(
                    name = "agent_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_agent_categories_agent")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "category_id",
                    referencedColumnName = "id",
                    foreignKey = @ForeignKey(name = "fk_agent_categories_category")
            ),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_agent_category",
                            columnNames = {"agent_id", "category_id"}
                    )
            },
            indexes = {
                    @Index(name = "idx_agent_categories_agent", columnList = "agent_id"),
                    @Index(name = "idx_agent_categories_category", columnList = "category_id")
            }
    )
    private Set<Category> categories = new HashSet<>();

    @Column(name = "last_assigned_at")
    private Instant lastAssignedAt;

    public void addCategory(Category category) {
        if (category != null) {
            this.categories.add(category);
        }
    }

    public void removeCategory(Category category) {
        if (category != null) {
            this.categories.remove(category);
        }
    }

    /**
     * Backward-compatible convenience method to set a single category.
     */
    public void setCategory(Category category) {
        this.categories.clear();
        if (category != null) {
            this.categories.add(category);
        }
    }

    /**
     * Maintained by MySQL triggers.
     * Hibernate only reads this field.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "ticket_status_counts",
            columnDefinition = "JSON",
            nullable = true,
            insertable = false,
            updatable = false
    )
    private String ticketStatusCounts;
}