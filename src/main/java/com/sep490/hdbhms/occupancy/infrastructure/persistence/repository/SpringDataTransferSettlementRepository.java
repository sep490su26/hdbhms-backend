package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTransferSettlementRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.TransferSettlementPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataTransferSettlementRepository implements TransferSettlementRepository {
    JpaTransferSettlementRepository jpaTransferSettlementRepository;
    TransferSettlementPersistenceMapper transferSettlementPersistenceMapper;

    @Override
    public TransferSettlement save(TransferSettlement transferSettlement) {
        return transferSettlementPersistenceMapper.toDomain(
                jpaTransferSettlementRepository.save(
                        transferSettlementPersistenceMapper.toEntity(
                                transferSettlement
                        )
                )
        );
    }
}
