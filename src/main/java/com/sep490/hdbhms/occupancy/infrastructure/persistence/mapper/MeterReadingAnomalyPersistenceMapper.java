package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.MeterReadingAnomaly;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
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
public class MeterReadingAnomalyPersistenceMapper {

    JpaMeterReadingBatchRepository jpaMeterReadingBatchRepository;
    JpaMeterReadingRepository jpaMeterReadingRepository;
    JpaUserRepository jpaUserRepository;

    public MeterReadingAnomaly toDomain(MeterReadingAnomalyEntity entity) {
        if (entity == null) return null;
        return MeterReadingAnomaly.builder()
                .id(entity.getId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getId() : null)
                .meterReadingId(entity.getMeterReading() != null ? entity.getMeterReading().getId() : null)
                .anomalyType(entity.getAnomalyType())
                .message(entity.getMessage())
                .severity(entity.getSeverity())
                .resolvedAt(entity.getResolvedAt())
                .resolvedById(entity.getResolvedBy() != null ? entity.getResolvedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MeterReadingAnomalyEntity toEntity(MeterReadingAnomaly domain) {
        if (domain == null) return null;
        return MeterReadingAnomalyEntity.builder()
                .id(domain.getId())
                .batch(domain.getBatchId() != null
                        ? jpaMeterReadingBatchRepository.findById(domain.getBatchId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_BATCH_NOT_FOUND))
                        : null)
                .meterReading(domain.getMeterReadingId() != null
                        ? jpaMeterReadingRepository.findById(domain.getMeterReadingId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_ANOMALY_NOT_FOUND))
                        : null)
                .anomalyType(domain.getAnomalyType())
                .message(domain.getMessage())
                .severity(domain.getSeverity())
                .resolvedAt(domain.getResolvedAt())
                .resolvedBy(domain.getResolvedById() != null
                        ? jpaUserRepository.findById(domain.getResolvedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
