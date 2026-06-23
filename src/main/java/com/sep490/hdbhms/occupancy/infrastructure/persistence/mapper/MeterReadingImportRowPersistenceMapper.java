package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.MeterReadingImportRow;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingImportRowEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingImportRowPersistenceMapper {

    JpaMeterReadingBatchRepository jpaMeterReadingBatchRepository;

    public MeterReadingImportRow toDomain(MeterReadingImportRowEntity entity) {
        if (entity == null) return null;
        return MeterReadingImportRow.builder()
                .id(entity.getId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getId() : null)
                .rowNo(entity.getRowNo())
                .roomCode(entity.getRoomCode())
                .meterType(entity.getMeterType())
                .previousValue(entity.getPreviousValue())
                .currentValue(entity.getCurrentValue())
                .validationStatus(entity.getValidationStatus())
                .validationMessage(entity.getValidationMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MeterReadingImportRowEntity toEntity(MeterReadingImportRow domain) {
        if (domain == null) return null;
        return MeterReadingImportRowEntity.builder()
                .id(domain.getId())
                .batch(domain.getBatchId() != null
                        ? jpaMeterReadingBatchRepository.findById(domain.getBatchId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.METER_READING_IMPORT_ROW_NOT_FOUND))
                        : null)
                .rowNo(domain.getRowNo())
                .roomCode(domain.getRoomCode())
                .meterType(domain.getMeterType())
                .previousValue(domain.getPreviousValue())
                .currentValue(domain.getCurrentValue())
                .validationStatus(domain.getValidationStatus())
                .validationMessage(domain.getValidationMessage())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
