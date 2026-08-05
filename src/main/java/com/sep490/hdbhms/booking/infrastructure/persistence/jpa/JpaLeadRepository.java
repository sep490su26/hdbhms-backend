package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.domain.model.Lead;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaLeadRepository extends JpaRepository<LeadEntity, Long> {
    Optional<LeadEntity> findByUser_Id(Long assignedUserId);
}
