package com.sep490.hdbhms.occupancy.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tenant {
    Long id;
    Long userId;
    Long propertyId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
    Short activeTenantToken;

    public static Tenant newTenant(Long propertyId, Long userId) {
        return Tenant.builder()
                .propertyId(propertyId)
                .userId(userId)
                .build();
    }
}
