package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.PersonProfilePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPersonProfileRepository implements PersonProfileRepository {
    JpaPersonProfileRepository jpaPersonProfileRepository;
    PersonProfilePersistenceMapper personProfilePersistenceMapper;

    @Override
    public PersonProfile save(PersonProfile personProfile) {
        return personProfilePersistenceMapper.toDomain(
                jpaPersonProfileRepository.save(
                        personProfilePersistenceMapper.toEntity(
                                personProfile
                        )
                )
        );
    }

    @Override
    public Optional<PersonProfile> findById(Long id) {
        return jpaPersonProfileRepository.findById(id)
                .map(personProfilePersistenceMapper::toDomain);
    }

    @Override
    public Optional<PersonProfile> findByUserId(Long userId) {
        return jpaPersonProfileRepository.findByUser_Id(userId)
                .map(personProfilePersistenceMapper::toDomain);
    }
}
