package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPersistenceMapper {


    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .mustChangePassword(entity.getMustChangePassword() != null && entity.getMustChangePassword())
                .status(entity.getStatus())
                .lastLoginAt(entity.getLastLoginAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public User toDomain(CreateUserCommand command) {
        return User.newUser(
                command.getPhone(),
                command.getEmail(),
                command.getPassword(),
                command.getInitialRole()
        );
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .role(domain.getRole())
                .mustChangePassword(domain.isMustChangePassword())
                .status(domain.getStatus())
                .lastLoginAt(domain.getLastLoginAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
