package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;

import java.util.Optional;

public interface PersonProfileRepository {
    PersonProfile save(PersonProfile personProfile);

    Optional<PersonProfile> findById(Long id);

    Optional<PersonProfile> findByUserId(Long userId);
    Optional<PersonProfile> findByPhone(String phone);
}
