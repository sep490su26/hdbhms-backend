package com.sep490.hdbhms.property.infrastructure.persistence.repository;

import com.sep490.hdbhms.property.application.port.out.PropertyImageRepository;
import com.sep490.hdbhms.property.domain.model.PropertyImage;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyImageRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.mapper.PropertyImagePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPropertyImageRepository implements PropertyImageRepository {
    JpaPropertyImageRepository jpaPropertyImageRepository;
    PropertyImagePersistenceMapper propertyImagePersistenceMapper;

    @Override
    public PropertyImage save(PropertyImage propertyImage) {
        return propertyImagePersistenceMapper.toDomain(
                jpaPropertyImageRepository.save(
                        propertyImagePersistenceMapper.toEntity(propertyImage)
                )
        );
    }

    @Override
    public Optional<PropertyImage> findById(Long id) {
        return jpaPropertyImageRepository.findById(id)
                .map(propertyImagePersistenceMapper::toDomain);
    }

    @Override
    public List<PropertyImage> findAllByPropertyId(Long propertyId) {
        return jpaPropertyImageRepository.findAllByProperty_IdOrderBySortOrderAscCreatedAtAscIdAsc(propertyId).stream()
                .map(propertyImagePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaPropertyImageRepository.deleteById(id);
    }
}
