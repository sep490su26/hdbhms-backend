package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.MeterReadingBatch;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
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
public class MeterReadingBatchPersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public MeterReadingBatch toDomain(MeterReadingBatchEntity entity) {
        if (entity == null) return null;
        return MeterReadingBatch.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .readingPeriod(entity.getReadingPeriod())
                .source(entity.getSource())
                .status(entity.getStatus())
                .importedFileId(entity.getImportedFile() != null ? entity.getImportedFile().getId() : null)
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .confirmedById(entity.getConfirmedBy() != null ? entity.getConfirmedBy().getId() : null)
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MeterReadingBatchEntity toEntity(MeterReadingBatch domain) {
        if (domain == null) return null;
        return MeterReadingBatchEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .readingPeriod(domain.getReadingPeriod())
                .source(domain.getSource())
                .status(domain.getStatus())
                .importedFile(domain.getImportedFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getImportedFileId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.findById(domain.getCreatedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .confirmedBy(domain.getConfirmedById() != null
                        ? jpaUserRepository.findById(domain.getConfirmedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .confirmedAt(domain.getConfirmedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
