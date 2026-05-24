package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.DebtSnapshot;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtSnapshotEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DebtSnapshotPersistenceMapper {
    JpaRoomRepository jpaRoomRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;

    public DebtSnapshot toDomain(DebtSnapshotEntity entity) {
        if (entity == null) return null;
        return DebtSnapshot.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .snapshotDate(entity.getSnapshotDate())
                .rentDebtAmount(entity.getRentDebtAmount())
                .utilityDebtAmount(entity.getUtilityDebtAmount())
                .otherDebtAmount(entity.getOtherDebtAmount())
                .rentDebtMonths(entity.getRentDebtMonths())
                .utilityDebtMonths(entity.getUtilityDebtMonths())
                .mixedDebtAmount(entity.getMixedDebtAmount())
                .debtLimitAmount(entity.getDebtLimitAmount())
                .isOverLimit(entity.getIsOverLimit())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public DebtSnapshotEntity toEntity(DebtSnapshot domain) {
        if (domain == null) return null;
        return DebtSnapshotEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.getReferenceById(domain.getRoomId())
                        : null)
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.getReferenceById(domain.getContractId())
                        : null)
                .snapshotDate(domain.getSnapshotDate())
                .rentDebtAmount(domain.getRentDebtAmount())
                .utilityDebtAmount(domain.getUtilityDebtAmount())
                .otherDebtAmount(domain.getOtherDebtAmount())
                .rentDebtMonths(domain.getRentDebtMonths())
                .utilityDebtMonths(domain.getUtilityDebtMonths())
                .mixedDebtAmount(domain.getMixedDebtAmount())
                .debtLimitAmount(domain.getDebtLimitAmount())
                .isOverLimit(domain.getIsOverLimit())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
