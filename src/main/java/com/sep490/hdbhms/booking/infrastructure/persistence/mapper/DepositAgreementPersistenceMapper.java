package com.sep490.hdbhms.booking.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.booking.domain.model.DepositAgreement;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositFormEntity;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaDepositFormRepository;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaLeadRepository;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaRoomHoldRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTenantRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositAgreementPersistenceMapper {
    JpaRoomRepository jpaRoomRepository;
    JpaLeadRepository jpaLeadRepository;
    JpaTenantRepository jpaTenantRepository;
    JpaRoomHoldRepository jpaRoomHoldRepository;
    JpaDepositFormRepository jpaDepositFormRepository;
    JpaPersonProfileRepository jpaPersonProfileRepository;

    public DepositAgreement toDomain(DepositFormEntity entity) {
        if (entity == null) return null;
        return DepositAgreement.builder()
                .id(entity.getId())
                .depositCode(entity.getDepositCode())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .depositFormId(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .leadId(entity.getLead() != null ? entity.getLead().getId() : null)
                .depositorPersonProfileId(entity.getDepositorPersonProfile() != null ? entity.getDepositorPersonProfile().getId() : null)
                .roomHoldId(entity.getRoomHold() != null ? entity.getRoomHold().getId() : null)
                .amount(entity.getAmount())
                .expectedMoveInDate(entity.getExpectedMoveInDate())
                .expectedLeaseSignDate(entity.getExpectedLeaseSignDate())
                .paymentDueAt(entity.getPaymentDueAt())
                .depositExpiresAt(entity.getDepositExpiresAt())
                .extensionCount(entity.getExtensionCount())
                .maxExtensions(entity.getMaxExtensions())
                .status(entity.getDepositStatus())
                .confirmedAt(entity.getConfirmedAt())
                .note(entity.getNote())
                .forfeitureReason(entity.getForfeitureReason())
                .refundedAmount(entity.getRefundedAmount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DepositFormEntity toEntity(DepositAgreement domain) {
        if (domain == null) return null;
        Long formId = domain.getId() != null ? domain.getId() : domain.getDepositFormId();
        DepositFormEntity entity = formId == null
                ? new DepositFormEntity()
                : jpaDepositFormRepository.findById(formId)
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        entity.setDepositCode(domain.getDepositCode());
        entity.setRoom(domain.getRoomId() == null ? entity.getRoom() : jpaRoomRepository.findById(domain.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND)));
        entity.setTenant(domain.getTenantId() == null ? null : jpaTenantRepository.findById(domain.getTenantId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND)));
        entity.setLead(domain.getLeadId() == null ? null : jpaLeadRepository.findById(domain.getLeadId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND)));
        entity.setDepositorPersonProfile(domain.getDepositorPersonProfileId() == null ? null : jpaPersonProfileRepository.findById(domain.getDepositorPersonProfileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND)));
        entity.setRoomHold(domain.getRoomHoldId() == null ? null : jpaRoomHoldRepository.findById(domain.getRoomHoldId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND)));
        entity.setAmount(domain.getAmount());
        entity.setExpectedMoveInDate(domain.getExpectedMoveInDate());
        entity.setExpectedLeaseSignDate(domain.getExpectedLeaseSignDate());
        entity.setPaymentDueAt(domain.getPaymentDueAt());
        entity.setDepositExpiresAt(domain.getDepositExpiresAt());
        entity.setExtensionCount(domain.getExtensionCount());
        entity.setMaxExtensions(domain.getMaxExtensions());
        entity.setDepositStatus(domain.getStatus());
        entity.setConfirmedAt(domain.getConfirmedAt());
        entity.setNote(domain.getNote());
        entity.setForfeitureReason(domain.getForfeitureReason());
        entity.setRefundedAmount(domain.getRefundedAmount());
        return entity;
    }
}
