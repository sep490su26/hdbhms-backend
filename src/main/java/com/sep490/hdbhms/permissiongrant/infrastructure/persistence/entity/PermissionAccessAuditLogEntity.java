package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity;

import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.permissiongrant.domain.valueObjects.PermissionAccessAction;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "permission_access_audit_logs",
        indexes = {
                @Index(name = "idx_permission_audit_target", columnList = "target_type, target_id, viewed_at"),
                @Index(name = "idx_permission_audit_viewer", columnList = "viewer_user_id, viewed_at"),
                @Index(name = "idx_permission_audit_grant", columnList = "permission_grant_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionAccessAuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_access_audit_log_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_grant_id", nullable = false)
    PermissionGrantEntity permissionGrant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewer_user_id", nullable = false)
    UserEntity viewer;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 100)
    TargetType targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    PermissionAccessAction action;

    @Column(columnDefinition = "TEXT")
    String reason;

    @Column(name = "ip_address", length = 100)
    String ipAddress;

    @Column(name = "user_agent", length = 1000)
    String userAgent;

    @Column(name = "viewed_at", nullable = false)
    LocalDateTime viewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
