package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomImageEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
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
public class RoomImagePersistenceMapper {

    JpaRoomRepository jpaRoomRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public RoomImage toDomain(RoomImageEntity entity) {
        if (entity == null) return null;
        return RoomImage.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .fileId(entity.getFile() != null ? entity.getFile().getId() : null)
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .fallback(false)
                .sourceRoomCode(null)
                .fallbackUrl(null)
                .build();
    }

    public RoomImageEntity toEntity(RoomImage domain) {
        if (domain == null) return null;
        return RoomImageEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_IMAGE_NOT_FOUND))
                        : null)
                .file(domain.getFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_IMAGE_NOT_FOUND))
                        : null)
                .sortOrder(domain.getSortOrder())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}