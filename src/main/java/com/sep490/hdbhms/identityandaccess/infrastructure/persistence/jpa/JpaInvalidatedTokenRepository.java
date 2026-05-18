package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.InvalidatedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvalidatedTokenRepository extends JpaRepository<InvalidatedTokenEntity, String> {
}
