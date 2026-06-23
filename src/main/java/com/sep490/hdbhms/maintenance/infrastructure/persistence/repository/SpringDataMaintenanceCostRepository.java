package com.sep490.hdbhms.maintenance.infrastructure.persistence.repository;

import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceCostRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceCost;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceCostRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper.MaintenanceCostPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataMaintenanceCostRepository implements MaintenanceCostRepository {
    JpaMaintenanceCostRepository jpaMaintenanceCostRepository;
    MaintenanceCostPersistenceMapper maintenanceCostPersistenceMapper;

    @Override
    public MaintenanceCost save(MaintenanceCost maintenanceCost) {
        return maintenanceCostPersistenceMapper.toDomain(
                jpaMaintenanceCostRepository.save(
                        maintenanceCostPersistenceMapper.toEntity(maintenanceCost)
                )
        );
    }

    @Override
    public List<MaintenanceCost> findAllByTicketId(Long ticketId) {
        return jpaMaintenanceCostRepository.findAllByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(maintenanceCostPersistenceMapper::toDomain)
                .toList();
    }
}
