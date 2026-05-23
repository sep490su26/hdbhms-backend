package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.PropertyPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

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
}
