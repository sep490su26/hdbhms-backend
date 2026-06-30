package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.VehicleStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaVehicleRepository extends JpaRepository<VehicleEntity, Long> {
    List<VehicleEntity> findByProfile_IdAndStatus(Long profileId, VehicleStatus status);
}
