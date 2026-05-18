package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.model.IdentityDocument;

import java.util.Optional;

public interface IdentityDocumentRepository {
    IdentityDocument save(IdentityDocument identityDocument);

    Optional<IdentityDocument> findById(Long id);
}
