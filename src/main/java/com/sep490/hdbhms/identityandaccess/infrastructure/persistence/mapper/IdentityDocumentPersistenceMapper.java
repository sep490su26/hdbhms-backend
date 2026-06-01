package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.IdentityDocument;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.IdentityDocumentEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityDocumentPersistenceMapper {
    JpaPersonProfileRepository jpaPersonProfileRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public IdentityDocument toDomain(IdentityDocumentEntity entity) {
        if (entity == null) return null;
        return IdentityDocument.builder()
                .id(entity.getId())
                .profileId(entity.getProfile() != null ? entity.getProfile().getId() : null)
                .docType(entity.getDocType())
                .docNumber(entity.getDocNumber())
                .issuedDate(entity.getIssuedDate())
                .issuedPlace(entity.getIssuedPlace())
                .expiryDate(entity.getExpiryDate())
                .rawOcrData(entity.getRawOcrData())
                .frontFileId(entity.getFrontFile() != null ? entity.getFrontFile().getId() : null)
                .backFileId(entity.getBackFile() != null ? entity.getBackFile().getId() : null)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public IdentityDocumentEntity toEntity(IdentityDocument domain) {
        if (domain == null) return null;
        return IdentityDocumentEntity.builder()
                .id(domain.getId())
                .profile(domain.getProfileId() != null
                        ? jpaPersonProfileRepository.findById(domain.getProfileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .docType(domain.getDocType())
                .docNumber(domain.getDocNumber())
                .issuedDate(domain.getIssuedDate())
                .issuedPlace(domain.getIssuedPlace())
                .expiryDate(domain.getExpiryDate())
                .rawOcrData(domain.getRawOcrData())
                .frontFile(domain.getFrontFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getFrontFileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .backFile(domain.getBackFileId() != null
                        ? jpaFileMetadataRepository.findById(domain.getBackFileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}