package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "permission_requests",
        indexes = {
                @Index(name = "idx_perm_tenant_status", columnList = "status, created_at"),
                @Index(name = "idx_perm_target", columnList = "target_type, target_id")
        }
)
public class PermissionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    UserEntity requesterUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    PermissionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @Column(name = "rejected_reason", length = 1000)
    String rejectedReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    PermissionRequestStatus status = PermissionRequestStatus.PENDING;

    @Column(name = "expires_at")
    LocalDateTime expiresAt;

    @Column(name = "decided_at")
    LocalDateTime decidedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
