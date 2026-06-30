package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.RolePromotionStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
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
        name = "role_promotions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_membership_active", columnNames = {"user_id", "role", "active_unique_token"})
        },
        indexes = {
                @Index(name = "idx_membership_tenant_role", columnList = "role, status"),
                @Index(name = "idx_membership_property", columnList = "property_id, role")
        }
)
public class RolePromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_promotion_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    PromotionRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    RolePromotionStatus status = RolePromotionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    PropertyEntity property;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @Column(name = "active_unique_token", insertable = false, updatable = false, columnDefinition = "TINYINT")
    Short activeUniqueToken;
}