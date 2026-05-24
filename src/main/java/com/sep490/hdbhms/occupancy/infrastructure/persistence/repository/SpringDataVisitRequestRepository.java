package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaVisitRequestRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.VisitRequestPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataVisitRequestRepository implements VisitRequestRepository {
    JpaVisitRequestRepository jpaVisitRequestRepository;
    VisitRequestPersistenceMapper visitRequestPersistenceMapper;

    @Override
    public VisitRequest save(VisitRequest visitRequest) {
        return visitRequestPersistenceMapper.toDomain(
                jpaVisitRequestRepository.save(
                        visitRequestPersistenceMapper.toEntity(visitRequest)
                )
        );
    }

    @Override
    public Optional<VisitRequest> findById(Long id) {
        return jpaVisitRequestRepository.findById(id)
                .map(visitRequestPersistenceMapper::toDomain);
    }
}
