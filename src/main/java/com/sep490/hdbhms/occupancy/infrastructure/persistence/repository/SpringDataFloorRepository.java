package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.FloorPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataFloorRepository implements FloorRepository {
    JpaFloorRepository jpaFloorRepository;
    FloorPersistenceMapper floorPersistenceMapper;

    @Override
    public Floor save(Floor floor) {
        return floorPersistenceMapper.toDomain(
                jpaFloorRepository.save(
                        floorPersistenceMapper.toEntity(
                                floor
                        )
                )
        );
    }

    @Override
    public Optional<Floor> findById(Long id) {
        return jpaFloorRepository.findById(id)
                .map(floorPersistenceMapper::toDomain);
    }

    @Override
    public List<Floor> findAllByPropertyId(Long propertyId) {
        return jpaFloorRepository.findAllByProperty_Id(propertyId)
                .stream().map(floorPersistenceMapper::toDomain)
                .sorted(Comparator.comparing(Floor::getSortOrder))
                .toList();
    }
}
