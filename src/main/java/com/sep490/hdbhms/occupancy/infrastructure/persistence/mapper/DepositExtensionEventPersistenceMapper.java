package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.DepositExtensionEvent;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositExtensionEventEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositAgreementRepository;
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
public class DepositExtensionEventPersistenceMapper {

    JpaDepositAgreementRepository jpaDepositAgreementRepository;
    JpaUserRepository jpaUserRepository;

    public DepositExtensionEvent toDomain(DepositExtensionEventEntity entity) {
        if (entity == null) return null;
        return DepositExtensionEvent.builder()
                .id(entity.getId())
                .depositAgreementId(entity.getDepositAgreement() != null ? entity.getDepositAgreement().getId() : null)
                .oldExpectedMoveInDate(entity.getOldExpectedMoveInDate())
                .newExpectedMoveInDate(entity.getNewExpectedMoveInDate())
                .oldExpiresAt(entity.getOldExpiresAt())
                .newExpiresAt(entity.getNewExpiresAt())
                .reason(entity.getReason())
                .approvedById(entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null)
                .approvedAt(entity.getApprovedAt())
                .build();
    }

    public DepositExtensionEventEntity toEntity(DepositExtensionEvent domain) {
        if (domain == null) return null;
        return DepositExtensionEventEntity.builder()
                .id(domain.getId())
                .depositAgreement(domain.getDepositAgreementId() != null
                        ? jpaDepositAgreementRepository.findById(domain.getDepositAgreementId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_EXTENSION_EVENT_NOT_FOUND))
                        : null)
                .oldExpectedMoveInDate(domain.getOldExpectedMoveInDate())
                .newExpectedMoveInDate(domain.getNewExpectedMoveInDate())
                .oldExpiresAt(domain.getOldExpiresAt())
                .newExpiresAt(domain.getNewExpiresAt())
                .reason(domain.getReason())
                .approvedBy(domain.getApprovedById() != null
                        ? jpaUserRepository.findById(domain.getApprovedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .approvedAt(domain.getApprovedAt())
                .build();
    }
}
