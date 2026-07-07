package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.mapper;

import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity.PermissionGrantEntity;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionGrantPersistenceMapper {
    EntityManager entityManager;

    public PermissionGrant toDomain(PermissionGrantEntity entity) {
        if (entity == null) return null;
        return PermissionGrant.builder()
                .id(entity.getId())
                .granteeUserId(entity.getGrantee() == null ? null : entity.getGrantee().getId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .sourceChangeRequestId(entity.getSourceChangeRequest() == null ? null : entity.getSourceChangeRequest().getId())
                .grantedBy(entity.getGrantedBy() == null ? null : entity.getGrantedBy().getId())
                .reason(entity.getReason())
                .durationCode(entity.getDurationCode())
                .grantedAt(entity.getGrantedAt())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .revokedBy(entity.getRevokedBy() == null ? null : entity.getRevokedBy().getId())
                .revokeReason(entity.getRevokeReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PermissionGrantEntity toEntity(PermissionGrant domain) {
        if (domain == null) return null;
        return PermissionGrantEntity.builder()
                .id(domain.getId())
                .grantee(userReference(domain.getGranteeUserId()))
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .sourceChangeRequest(changeRequestReference(domain.getSourceChangeRequestId()))
                .grantedBy(userReference(domain.getGrantedBy()))
                .reason(domain.getReason())
                .durationCode(domain.getDurationCode())
                .grantedAt(domain.getGrantedAt())
                .expiresAt(domain.getExpiresAt())
                .revokedAt(domain.getRevokedAt())
                .revokedBy(userReference(domain.getRevokedBy()))
                .revokeReason(domain.getRevokeReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private UserEntity userReference(Long id) {
        return id == null ? null : entityManager.getReference(UserEntity.class, id);
    }

    private ChangeRequestEntity changeRequestReference(Long id) {
        return id == null ? null : entityManager.getReference(ChangeRequestEntity.class, id);
    }
}
