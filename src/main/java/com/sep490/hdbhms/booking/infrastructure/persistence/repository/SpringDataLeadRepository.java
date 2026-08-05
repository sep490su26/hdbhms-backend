package com.sep490.hdbhms.booking.infrastructure.persistence.repository;

import com.sep490.hdbhms.booking.application.port.out.LeadRepository;
import com.sep490.hdbhms.booking.domain.model.Lead;
import com.sep490.hdbhms.booking.infrastructure.persistence.jpa.JpaLeadRepository;
import com.sep490.hdbhms.booking.infrastructure.persistence.mapper.LeadPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataLeadRepository implements LeadRepository {
    JpaLeadRepository jpaLeadRepository;
    LeadPersistenceMapper leadPersistenceMapper;

    @Override
    public Lead save(Lead lead) {
        return leadPersistenceMapper.toDomain(
                jpaLeadRepository.save(
                        leadPersistenceMapper.toEntity(lead)
                )
        );
    }

    @Override
    public Optional<Lead> findById(Long id) {
        return jpaLeadRepository.findById(id)
                .map(leadPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Lead> findByAssignedUserId(Long assignedUserId) {
        return jpaLeadRepository.findByUser_Id(assignedUserId)
                .map(leadPersistenceMapper::toDomain);
    }
}
