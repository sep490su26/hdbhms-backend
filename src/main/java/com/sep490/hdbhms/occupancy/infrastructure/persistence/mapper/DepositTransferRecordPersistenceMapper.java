package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.DepositTransferRecord;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositTransferRecordEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomTransferRequestRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositTransferRecordPersistenceMapper {

    JpaRoomTransferRequestRepository jpaRoomTransferRequestRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaDepositAgreementRepository jpaDepositAgreementRepository;
    JpaRoomRepository jpaRoomRepository;

    public DepositTransferRecord toDomain(DepositTransferRecordEntity entity) {
        if (entity == null) return null;
        return DepositTransferRecord.builder()
                .id(entity.getId())
                .transferRequestId(entity.getTransferRequest() != null ? entity.getTransferRequest().getId() : null)
                .oldContractId(entity.getOldContract() != null ? entity.getOldContract().getId() : null)
                .newContractId(entity.getNewContract() != null ? entity.getNewContract().getId() : null)
                .oldDepositAgreementId(entity.getOldDepositAgreement() != null ? entity.getOldDepositAgreement().getId() : null)
                .fromRoomId(entity.getFromRoom() != null ? entity.getFromRoom().getId() : null)
                .toRoomId(entity.getToRoom() != null ? entity.getToRoom().getId() : null)
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .effectiveDate(entity.getEffectiveDate())
                .cancelledAt(entity.getCancelledAt())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DepositTransferRecordEntity toEntity(DepositTransferRecord domain) {
        if (domain == null) return null;
        return DepositTransferRecordEntity.builder()
                .id(domain.getId())
                .transferRequest(domain.getTransferRequestId() != null
                        ? jpaRoomTransferRequestRepository.findById(domain.getTransferRequestId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND))
                        : null)
                .oldContract(domain.getOldContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getOldContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .newContract(domain.getNewContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getNewContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND))
                        : null)
                .oldDepositAgreement(domain.getOldDepositAgreementId() != null
                        ? jpaDepositAgreementRepository.findById(domain.getOldDepositAgreementId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND))
                        : null)
                .fromRoom(domain.getFromRoomId() != null
                        ? jpaRoomRepository.findById(domain.getFromRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND))
                        : null)
                .toRoom(domain.getToRoomId() != null
                        ? jpaRoomRepository.findById(domain.getToRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND))
                        : null)
                .amount(domain.getAmount())
                .status(domain.getStatus())
                .effectiveDate(domain.getEffectiveDate())
                .cancelledAt(domain.getCancelledAt())
                .note(domain.getNote())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
