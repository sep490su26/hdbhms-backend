package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.ContractHandoverRecord;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
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
public class ContractHandoverRecordPersistenceMapper {

    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaMeterReadingRepository jpaMeterReadingRepository;
    JpaUserRepository jpaUserRepository;

    public ContractHandoverRecord toDomain(ContractHandoverRecordEntity entity) {
        if (entity == null) return null;
        return ContractHandoverRecord.builder()
                .id(entity.getId())
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .handoverType(entity.getHandoverType())
                .handoverDate(entity.getHandoverDate())
                .electricityReadingId(entity.getElectricityReading() != null ? entity.getElectricityReading().getId() : null)
                .waterReadingId(entity.getWaterReading() != null ? entity.getWaterReading().getId() : null)
                .note(entity.getNote())
                .status(entity.getStatus())
                .confirmedById(entity.getConfirmedBy() != null ? entity.getConfirmedBy().getId() : null)
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ContractHandoverRecordEntity toEntity(ContractHandoverRecord domain) {
        if (domain == null) return null;
        return ContractHandoverRecordEntity.builder()
                .id(domain.getId())
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND))
                        : null)
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND))
                        : null)
                .handoverType(domain.getHandoverType())
                .handoverDate(domain.getHandoverDate())
                .electricityReading(domain.getElectricityReadingId() != null
                        ? jpaMeterReadingRepository.findById(domain.getElectricityReadingId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND))
                        : null)
                .waterReading(domain.getWaterReadingId() != null
                        ? jpaMeterReadingRepository.findById(domain.getWaterReadingId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_HANDOVER_RECORD_NOT_FOUND))
                        : null)
                .note(domain.getNote())
                .status(domain.getStatus())
                .confirmedBy(domain.getConfirmedById() != null
                        ? jpaUserRepository.findById(domain.getConfirmedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .confirmedAt(domain.getConfirmedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
