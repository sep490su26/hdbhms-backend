package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaVisitRequestRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.VisitRequestPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.VisitRequestSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public List<Long> findIdsByFullText(String keyword) {
        return jpaVisitRequestRepository.findIdsByFullText(keyword);
    }

    @Override
    public Page<VisitRequest> findAll(
            List<Long> ids,
            String propertyCode,
            String roomCode,
            Long propertyId,
            Long roomId,
            VisitRequestStatus status,
            LocalDateTime from,
            LocalDateTime localDateTime,
            Pageable pageable
    ) {
        Specification<VisitRequestEntity> specification = Specification
                .where(VisitRequestSpecifications.idIn(ids))
                .and(VisitRequestSpecifications.hasPropertyCode(propertyCode))
                .and(VisitRequestSpecifications.hasRoomCode(roomCode))
                .and(VisitRequestSpecifications.hasPropertyId(propertyId))
                .and(VisitRequestSpecifications.hasRoomId(roomId))
                .and(VisitRequestSpecifications.hasStatus(status))
                .and(VisitRequestSpecifications.preferredStartBetween(from, localDateTime))
                .and(VisitRequestSpecifications.notDeleted());
        return jpaVisitRequestRepository.findAll(specification, pageable)
                .map(visitRequestPersistenceMapper::toDomain);
    }
}
