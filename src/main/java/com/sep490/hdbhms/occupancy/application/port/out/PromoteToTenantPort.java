package com.sep490.hdbhms.occupancy.application.port.out;

public interface PromoteToTenantPort {
    void execute(Long propertyId, Long userId);
}
