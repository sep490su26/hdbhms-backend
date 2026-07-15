package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.occupancy.domain.value_objects.RuleStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "property_rules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_property_rule_code", columnNames = {"property_id", "rule_code"})
        },
        indexes = {
                @Index(name = "idx_property_rules", columnList = "property_id, status")
        }
)
public class PropertyRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_rule_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    PropertyEntity property;

    @Column(name = "rule_code", nullable = false, length = 50)
    String ruleCode;

    @Column(nullable = false, length = 255)
    String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    String description;

    @Column(name = "default_fine_amount")
    Long defaultFineAmount;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    RuleStatus status = RuleStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}