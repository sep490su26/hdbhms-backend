package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.IdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.IdentityDocument;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaIdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.IdentityDocumentPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataIdentityDocumentRepository implements IdentityDocumentRepository {
    JpaIdentityDocumentRepository jpaIdentityDocumentRepository;
    IdentityDocumentPersistenceMapper identityDocumentPersistenceMapper;

    @Override
    public IdentityDocument save(IdentityDocument identityDocument) {
        return identityDocumentPersistenceMapper.toDomain(
                jpaIdentityDocumentRepository.save(
                        identityDocumentPersistenceMapper.toEntity(
                                identityDocument
                        )
                )
        );
    }

    @Override
    public Optional<IdentityDocument> findById(Long id) {
        return jpaIdentityDocumentRepository.findById(id)
                .map(identityDocumentPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByDocTypeAndDocNumber(DocumentType documentType, String idNumber) {
        return jpaIdentityDocumentRepository.existsByDocTypeAndDocNumber(documentType, idNumber);
    }

    @Override
    public boolean existsByProfileIdAndDocType(Long profileId, DocumentType documentType) {
        return jpaIdentityDocumentRepository.existsByProfile_IdAndDocType(profileId, documentType);
    }
}
