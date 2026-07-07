package com.sep490.hdbhms.maintenance.infrastructure.persistence.repository;

import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.valueObjects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper.MaintenanceTicketPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.MaintenanceTicketSpecifications;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataMaintenanceTicketRepository implements MaintenanceTicketRepository {
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    MaintenanceTicketPersistenceMapper maintenanceTicketPersistenceMapper;

    @Override
    public MaintenanceTicket save(MaintenanceTicket maintenanceTicket) {
        return maintenanceTicketPersistenceMapper.toDomain(
                jpaMaintenanceTicketRepository.save(
                        maintenanceTicketPersistenceMapper.toEntity(maintenanceTicket)
                )
        );
    }

    @Override
    public Optional<MaintenanceTicket> findById(Long id) {
        return jpaMaintenanceTicketRepository.findById(id)
                .map(maintenanceTicketPersistenceMapper::toDomain);
    }

    @Override
    public List<Long> findIdsByTicketCode(String code) {
        return jpaMaintenanceTicketRepository.findIdsByTicketCode(code);
    }

    @Override
    public Page<MaintenanceTicket> findAll(
            List<Long> ids,
            String type,
            String status,
            Long roomId,
            Pageable pageable
    ) {
        Specification<MaintenanceTicketEntity> maintenanceTicketEntitySpecification = Specification
                .where(MaintenanceTicketSpecifications.idIn(ids))
                .and(MaintenanceTicketSpecifications.roomIdEquals(roomId));
        if (!StringUtils.isEmpty(type)) {
            maintenanceTicketEntitySpecification = maintenanceTicketEntitySpecification
                    .and(MaintenanceTicketSpecifications.categoryOrScopeEquals(type));
        }
        if (!StringUtils.isEmpty(status)) {
            maintenanceTicketEntitySpecification = maintenanceTicketEntitySpecification
                    .and(
                            MaintenanceTicketSpecifications
                                    .statusIn(MaintenanceTicketStatus.valueOf(status))
                    );
        }
        return jpaMaintenanceTicketRepository.findAll(maintenanceTicketEntitySpecification, pageable)
                .map(maintenanceTicketPersistenceMapper::toDomain);
    }
}
