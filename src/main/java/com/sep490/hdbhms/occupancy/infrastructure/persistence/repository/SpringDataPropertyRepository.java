package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.PropertyPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.PropertySpecifications;
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
public class SpringDataPropertyRepository implements PropertyRepository {
    JpaPropertyRepository jpaPropertyRepository;
    PropertyPersistenceMapper propertyPersistenceMapper;

    @Override
    public Property save(Property property) {
        return propertyPersistenceMapper.toDomain(
                jpaPropertyRepository.save(
                        propertyPersistenceMapper.toEntity(property)
                )
        );
    }

    @Override
    public Optional<Property> findById(Long id) {
        return jpaPropertyRepository.findById(id)
                .map(propertyPersistenceMapper::toDomain);
    }

    @Override
    public Page<Property> findAll(PropertyStatus status, Pageable pageable) {
        List<Long> ids = jpaPropertyRepository.findAll().stream()
                .map(PropertyEntity::getId)
                .toList();
        Specification<PropertyEntity> specification = Specification
                .where(PropertySpecifications.idIn(ids))
                .and(PropertySpecifications.statusIn(status));
        return jpaPropertyRepository.findAll(specification, pageable)
                .map(propertyPersistenceMapper::toDomain);
    }
}
