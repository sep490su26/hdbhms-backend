package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.MeterReading;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
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
public class MeterReadingPersistenceMapper {

    JpaMeterReadingBatchRepository jpaMeterReadingBatchRepository;
    JpaMeterRepository jpaMeterRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public MeterReading toDomain(MeterReadingEntity entity) {
        if (entity == null) return null;
        return MeterReading.builder()
                .id(entity.getId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getId() : null)
                .meterId(entity.getMeter() != null ? entity.getMeter().getId() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .readingPeriod(entity.getReadingPeriod())
                .revisionNo(entity.getRevisionNo())
                .previousValue(entity.getPreviousValue())
                .currentValue(entity.getCurrentValue())
                .usageAmount(entity.getUsageAmount())
                .readingDate(entity.getReadingDate())
                .photoFileId(entity.getPhotoFile() != null ? entity.getPhotoFile().getId() : null)
                .source(entity.getSource())
                .status(entity.getStatus())
                .voidReason(entity.getVoidReason())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .activeReadingKey(entity.getActiveReadingKey())
                .build();
    }

    public MeterReadingEntity toEntity(MeterReading domain) {
        if (domain == null) return null;
        return MeterReadingEntity.builder()
                .id(domain.getId())
                .batch(domain.getBatchId() != null
                        ? jpaMeterReadingBatchRepository.findById(domain.getBatchId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .meter(domain.getMeterId() != null
                        ? jpaMeterRepository.findById(domain.getMeterId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .readingPeriod(domain.getReadingPeriod())
                .revisionNo(domain.getRevisionNo())
                .previousValue(domain.getPreviousValue())
                .currentValue(domain.getCurrentValue())
                .readingDate(domain.getReadingDate())
                .photoFile(domain.getPhotoFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getPhotoFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .source(domain.getSource())
                .status(domain.getStatus())
                .voidReason(domain.getVoidReason())
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.findById(domain.getCreatedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
