package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.PropertyImage;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyImageEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyImagePersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public PropertyImage toDomain(PropertyImageEntity entity) {
        if (entity == null) return null;
        return PropertyImage.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .fileId(entity.getFile() != null ? entity.getFile().getId() : null)
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PropertyImageEntity toEntity(PropertyImage domain) {
        if (domain == null) return null;
        return PropertyImageEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .file(domain.getFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .sortOrder(domain.getSortOrder())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
