package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.DepositTransferRecordRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositTransferRecord;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaDepositTransferRecordRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.DepositTransferRecordPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataDepositTransferRecordRepository implements DepositTransferRecordRepository {
    JpaDepositTransferRecordRepository jpaDepositTransferRecordRepository;
    DepositTransferRecordPersistenceMapper depositTransferRecordPersistenceMapper;

    @Override
    public DepositTransferRecord save(DepositTransferRecord depositTransferRecord) {
        return depositTransferRecordPersistenceMapper.toDomain(
                jpaDepositTransferRecordRepository.save(
                        depositTransferRecordPersistenceMapper.toEntity(depositTransferRecord)
                )
        );
    }

    @Override
    public Optional<DepositTransferRecord> findByTransferRequestId(Long transferRequestId) {
        return jpaDepositTransferRecordRepository.findFirstByTransferRequest_IdOrderByIdDesc(transferRequestId)
                .map(depositTransferRecordPersistenceMapper::toDomain);
    }

    @Override
    public Optional<DepositTransferRecord> findByNewContractId(Long newContractId) {
        return jpaDepositTransferRecordRepository.findFirstByNewContract_IdOrderByIdDesc(newContractId)
                .map(depositTransferRecordPersistenceMapper::toDomain);
    }
}
