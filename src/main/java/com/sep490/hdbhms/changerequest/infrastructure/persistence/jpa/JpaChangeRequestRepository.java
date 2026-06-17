package com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa;

import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaChangeRequestRepository extends JpaRepository<ChangeRequestEntity, Long> {
}
