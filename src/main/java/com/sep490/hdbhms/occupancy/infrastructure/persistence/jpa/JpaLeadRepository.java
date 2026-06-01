package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.model.Lead;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaLeadRepository extends JpaRepository<LeadEntity, Long> {
    Optional<LeadEntity> findByUser_Id(Long assignedUserId);
}
