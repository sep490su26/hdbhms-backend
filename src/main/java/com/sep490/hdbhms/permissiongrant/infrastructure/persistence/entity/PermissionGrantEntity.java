package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity;

import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.permissiongrant.domain.valueObjects.PermissionGrantDurationCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "permission_grants",
        indexes = {
                @Index(name = "idx_permission_grant_lookup", columnList = "grantee_user_id, target_type, target_id, expires_at, revoked_at"),
                @Index(name = "idx_permission_grant_source_request", columnList = "source_change_request_id"),
                @Index(name = "idx_permission_grant_owner_review", columnList = "target_type, target_id, revoked_at, expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionGrantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_grant_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grantee_user_id", nullable = false)
    UserEntity grantee;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 100)
    TargetType targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_change_request_id")
    ChangeRequestEntity sourceChangeRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by", nullable = false)
    UserEntity grantedBy;

    @Column(columnDefinition = "TEXT")
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_code", nullable = false, length = 50)
    PermissionGrantDurationCode durationCode;

    @Column(name = "granted_at", nullable = false)
    LocalDateTime grantedAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    UserEntity revokedBy;

    @Column(name = "revoke_reason", columnDefinition = "TEXT")
    String revokeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
