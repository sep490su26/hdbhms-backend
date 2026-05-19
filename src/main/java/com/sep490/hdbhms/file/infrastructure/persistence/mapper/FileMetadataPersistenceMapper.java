package com.sep490.hdbhms.file.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileMetadataPersistenceMapper {
    public FileMetadata toDomain(FileMetadataEntity entity) {
        return FileMetadata.builder()
//                .id(entity.getId())
//                .ownerUserId(entity.getOwnerUuid())
//                .contentType(entity.getContentType())
//                .sizeBytes(entity.getSize())
//                .url(entity.getUrl())
//                .storageKey(entity.getPath())
//                .duration(entity.getDuration())
//                .sha256Checksum(entity.getMd5Checksum())
//                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    public FileMetadataEntity toEntity(FileMetadata domain) {
        return FileMetadataEntity.builder()
//                .id(domain.getId())
//                .ownerUuid(domain.getOwnerUserId())
//                .contentType(domain.getContentType())
//                .size(domain.getSizeBytes())
//                .url(domain.getUrl())
//                .path(domain.getStorageKey())
//                .duration(domain.getDuration())
//                .md5Checksum(domain.getSha256Checksum())
//                .uploadedAt(domain.getUploadedAt())
                .build();
    }
}
