package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonProfilePersistenceMapper {
    JpaUserRepository jpaUserRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public PersonProfile toDomain(PersonProfileEntity entity) {
        if (entity == null) return null;
        return PersonProfile.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .fullName(entity.getFullName())
                .dob(entity.getDob())
                .gender(entity.getGender())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .permanentAddress(entity.getPermanentAddress())
                .portraitFileId(entity.getPortraitFile() != null ? entity.getPortraitFile().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public PersonProfileEntity toEntity(PersonProfile domain) {
        if (domain == null) return null;
        return PersonProfileEntity.builder()
                .id(domain.getId())
                .user(domain.getUserId() != null
                        ? jpaUserRepository.findById(domain.getUserId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .fullName(domain.getFullName())
                .dob(domain.getDob())
                .gender(domain.getGender())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .permanentAddress(domain.getPermanentAddress())
                .portraitFile(domain.getPortraitFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getPortraitFileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}