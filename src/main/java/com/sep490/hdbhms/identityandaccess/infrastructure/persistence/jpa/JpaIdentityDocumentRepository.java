package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.IdentityDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaIdentityDocumentRepository extends JpaRepository<IdentityDocumentEntity, Long> {
}
