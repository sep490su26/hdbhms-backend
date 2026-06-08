package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.EmergencyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEmergencyContactRepository extends JpaRepository<EmergencyContactEntity, Long> {
    void deleteAllByTenantProfile_Id(Long profileId);
}
