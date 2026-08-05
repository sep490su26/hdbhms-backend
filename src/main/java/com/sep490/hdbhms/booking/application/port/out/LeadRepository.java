package com.sep490.hdbhms.booking.application.port.out;

import com.sep490.hdbhms.booking.domain.model.Lead;

import java.util.Optional;

public interface LeadRepository {
    Lead save(Lead lead);

    Optional<Lead> findById(Long id);

    Optional<Lead> findByAssignedUserId(Long id);
}
