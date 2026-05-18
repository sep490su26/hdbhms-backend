package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import org.springframework.stereotype.Component;

@Component
public class PropertyPersistenceMapper {

    public Property toDomain(PropertyEntity entity) {
        if (entity == null) return null;
        return Property.builder()
                .id(entity.getId())
                .propertyCode(entity.getPropertyCode())
                .name(entity.getName())
                .propertyType(entity.getPropertyType())
                .addressLine(entity.getAddressLine())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public PropertyEntity toEntity(Property domain) {
        if (domain == null) return null;
        return PropertyEntity.builder()
                .id(domain.getId())
                .propertyCode(domain.getPropertyCode())
                .name(domain.getName())
                .propertyType(domain.getPropertyType())
                .addressLine(domain.getAddressLine())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}