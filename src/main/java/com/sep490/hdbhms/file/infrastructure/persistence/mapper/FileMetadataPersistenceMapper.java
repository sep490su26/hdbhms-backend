package com.sep490.hdbhms.file.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileMetadataPersistenceMapper {
    JpaUserRepository jpaUserRepository;

    public FileMetadata toDomain(FileMetadataEntity entity) {
        if (entity == null) return null;
        return FileMetadata.builder()
                .id(entity.getId())
                .ownerUserId(entity.getOwner() != null ? entity.getOwner().getId() : null)
                .storageKey(entity.getStorageKey())
                .originalName(entity.getOriginalName())
                .mimeType(entity.getMimeType())
                .sizeBytes(entity.getSizeBytes())
                .sha256Checksum(entity.getSha256Checksum())
                .category(entity.getCategory())
                .isSensitive(entity.isSensitive())
                .createdAt(entity.getCreatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public FileMetadataEntity toEntity(FileMetadata domain) {
        if (domain == null) return null;
        return FileMetadataEntity.builder()
                .id(domain.getId())
                .owner(domain.getOwnerUserId() != null
                        ? jpaUserRepository.getReferenceById(domain.getOwnerUserId())
                        : null)
                .storageKey(domain.getStorageKey())
                .originalName(domain.getOriginalName())
                .mimeType(domain.getMimeType())
                .sizeBytes(domain.getSizeBytes())
                .sha256Checksum(domain.getSha256Checksum())
                .category(domain.getCategory())
                .isSensitive(domain.isSensitive())
                .createdAt(domain.getCreatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
