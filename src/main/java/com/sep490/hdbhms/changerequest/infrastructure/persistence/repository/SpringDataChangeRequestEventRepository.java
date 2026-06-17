package com.sep490.hdbhms.changerequest.infrastructure.persistence.repository;

import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestEventRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequestEvent;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestEventRepository;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.mapper.ChangeRequestEventPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataChangeRequestEventRepository implements ChangeRequestEventRepository {

    JpaChangeRequestEventRepository jpaChangeRequestEventRepository;
    ChangeRequestEventPersistenceMapper mapper;

    @Override
    public ChangeRequestEvent save(ChangeRequestEvent changeRequestEvent) {
        return mapper.toDomain(
                jpaChangeRequestEventRepository.save(
                        mapper.toEntity(changeRequestEvent)
                )
        );
    }

    @Override
    public Optional<ChangeRequestEvent> findById(Long id) {
        return jpaChangeRequestEventRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ChangeRequestEvent> findAllByRequestId(Long requestId) {
        return jpaChangeRequestEventRepository.findAllByChangeRequest_Id(requestId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
