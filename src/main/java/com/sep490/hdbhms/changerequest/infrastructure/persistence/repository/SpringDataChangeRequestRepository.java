package com.sep490.hdbhms.changerequest.infrastructure.persistence.repository;

import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestRepository;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.mapper.ChangeRequestPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataChangeRequestRepository implements ChangeRequestRepository {

    JpaChangeRequestRepository jpaChangeRequestRepository;
    ChangeRequestPersistenceMapper mapper;

    @Override
    public ChangeRequest save(ChangeRequest changeRequest) {
        return mapper.toDomain(
                jpaChangeRequestRepository.save(
                        mapper.toEntity(changeRequest)
                )
        );
    }

    @Override
    public Optional<ChangeRequest> findById(Long id) {
        return jpaChangeRequestRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ChangeRequest> findAll() {
        return jpaChangeRequestRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<ChangeRequest> findFiltered(RequestType type, RequestStatus status, String search, Pageable pageable) {
        return jpaChangeRequestRepository.findFiltered(type, status, search, pageable)
                .map(mapper::toDomain);
    }
}
