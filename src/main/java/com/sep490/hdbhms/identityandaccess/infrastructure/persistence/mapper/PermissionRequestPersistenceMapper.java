package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PermissionRequestEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionRequestPersistenceMapper {
    JpaUserRepository jpaUserRepository;

    public PermissionRequest toDomain(PermissionRequestEntity entity) {
        if (entity == null) return null;
        return PermissionRequest.builder()
                .id(entity.getId())
                .requesterUserId(entity.getRequesterUser() != null ? entity.getRequesterUser().getId() : null)
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .rejectedReason(entity.getRejectedReason())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .decidedAt(entity.getDecidedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PermissionRequestEntity toEntity(PermissionRequest domain) {
        if (domain == null) return null;
        return PermissionRequestEntity.builder()
                .id(domain.getId())
                .requesterUser(domain.getRequesterUserId() != null
                        ? jpaUserRepository.getReferenceById(domain.getRequesterUserId())
                        : null)
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .rejectedReason(domain.getRejectedReason())
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .decidedAt(domain.getDecidedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}