package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositFormEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositFormPersistenceMapper {

    JpaRoomRepository jpaRoomRepository;

    public DepositForm toDomain(DepositFormEntity entity) {
        if (entity == null) return null;
        return DepositForm.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .idNumber(entity.getIdNumber())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .expectedMoveInDate(entity.getExpectedMoveInDate())
                .expectedLeaseSignDate(entity.getExpectedLeaseSignDate())
                .paymentDueAt(entity.getPaymentDueAt())
                .depositExpiresAt(entity.getDepositExpiresAt())
                .status(entity.getStatus())
                .confirmedAt(entity.getConfirmedAt())
                .rejectReason(entity.getRejectReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public DepositFormEntity toEntity(DepositForm domain) {
        if (domain == null) return null;
        return DepositFormEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .idNumber(domain.getIdNumber())
                .fullName(domain.getFullName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .expectedMoveInDate(domain.getExpectedMoveInDate())
                .expectedLeaseSignDate(domain.getExpectedLeaseSignDate())
                .paymentDueAt(domain.getPaymentDueAt())
                .depositExpiresAt(domain.getDepositExpiresAt())
                .status(domain.getStatus())
                .confirmedAt(domain.getConfirmedAt())
                .rejectReason(domain.getRejectReason())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
