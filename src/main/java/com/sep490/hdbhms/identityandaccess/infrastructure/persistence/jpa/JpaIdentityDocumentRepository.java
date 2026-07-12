package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.IdentityDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaIdentityDocumentRepository extends JpaRepository<IdentityDocumentEntity, Long> {
    boolean existsByDocTypeAndDocNumber(DocumentType documentType, String idNumber);

    boolean existsByProfile_IdAndDocType(Long profileId, DocumentType documentType);

    Optional<IdentityDocumentEntity> findFirstByProfile_IdAndDocTypeAndStatusOrderByUpdatedAtDesc(
            Long profileId,
            DocumentType documentType,
            DocumentStatus status
    );
}
