package com.sep490.hdbhms.notification.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.domain.model.UserMobileDeviceToken;
import com.sep490.hdbhms.notification.infrastructure.persistence.entity.UserMobileDeviceTokenEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserMobileDeviceTokenPersistenceMapper {

    JpaUserRepository jpaUserRepository;

    public UserMobileDeviceToken toDomain(UserMobileDeviceTokenEntity entity) {
        if (entity == null) return null;
        return UserMobileDeviceToken.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .token(entity.getToken())
                .platform(entity.getPlatform())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UserMobileDeviceTokenEntity toEntity(UserMobileDeviceToken domain) {
        if (domain == null) return null;
        return UserMobileDeviceTokenEntity.builder()
                .id(domain.getId())
                .user(domain.getUserId() != null
                        ? jpaUserRepository.getReferenceById(domain.getUserId())
                        : null)
                .token(domain.getToken())
                .platform(domain.getPlatform())
                .isActive(domain.getIsActive())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
