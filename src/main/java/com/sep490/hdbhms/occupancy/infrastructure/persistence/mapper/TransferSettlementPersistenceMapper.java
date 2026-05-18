package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TransferSettlementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomTransferRequestRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
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
public class TransferSettlementPersistenceMapper {

    JpaRoomTransferRequestRepository jpaRoomTransferRequestRepository;
    JpaInvoiceRepository jpaInvoiceRepository;
    JpaUserRepository jpaUserRepository;

    public TransferSettlement toDomain(TransferSettlementEntity entity) {
        if (entity == null) return null;
        return TransferSettlement.builder()
                .id(entity.getId())
                .transferRequestId(entity.getTransferRequest() != null ? entity.getTransferRequest().getId() : null)
                .oldRoomRemainingValue(entity.getOldRoomRemainingValue())
                .newRoomRequiredValue(entity.getNewRoomRequiredValue())
                .differenceAmount(entity.getDifferenceAmount())
                .settlementType(entity.getSettlementType())
                .oldRoomFinalInvoiceId(entity.getOldRoomFinalInvoice() != null ? entity.getOldRoomFinalInvoice().getId() : null)
                .transferDifferenceInvoiceId(entity.getTransferDifferenceInvoice() != null ? entity.getTransferDifferenceInvoice().getId() : null)
                .confirmedById(entity.getConfirmedBy() != null ? entity.getConfirmedBy().getId() : null)
                .confirmedAt(entity.getConfirmedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public TransferSettlementEntity toEntity(TransferSettlement domain) {
        if (domain == null) return null;
        return TransferSettlementEntity.builder()
                .id(domain.getId())
                .transferRequest(domain.getTransferRequestId() != null
                        ? jpaRoomTransferRequestRepository.findById(domain.getTransferRequestId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .oldRoomRemainingValue(domain.getOldRoomRemainingValue())
                .newRoomRequiredValue(domain.getNewRoomRequiredValue())
                .differenceAmount(domain.getDifferenceAmount())
                .settlementType(domain.getSettlementType())
                .oldRoomFinalInvoice(domain.getOldRoomFinalInvoiceId() != null
                        ? jpaInvoiceRepository.findById(domain.getOldRoomFinalInvoiceId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .transferDifferenceInvoice(domain.getTransferDifferenceInvoiceId() != null
                        ? jpaInvoiceRepository.findById(domain.getTransferDifferenceInvoiceId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
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
