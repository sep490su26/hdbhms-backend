package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.CollectionAccount;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.CollectionAccountEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CollectionAccountPersistenceMapper {
    JpaPropertyRepository propertyRepository;
    
    public CollectionAccount toDomain(CollectionAccountEntity entity) {
        if (entity == null) return null;
        Long propertyId = null;
        if (entity.getProperty() != null) {
            propertyId = entity.getProperty().getId();
        }
        return CollectionAccount.builder()
                .id(entity.getId())
                .propertyId(propertyId)
                .accountType(entity.getAccountType())
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .accountHolder(entity.getAccountHolder())
                .provider(entity.getProvider())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    public CollectionAccountEntity toEntity(CollectionAccount domain) {
        if (domain == null) return null;
        var property = domain.getPropertyId() != null 
                ? propertyRepository.findById(domain.getPropertyId()).orElse(null) : null;
                
        return CollectionAccountEntity.builder()
                .id(domain.getId())
                .property(property)
                .accountType(domain.getAccountType())
                .bankName(domain.getBankName())
                .accountNumber(domain.getAccountNumber())
                .accountHolder(domain.getAccountHolder())
                .provider(domain.getProvider())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
