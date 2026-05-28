package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdentityDocument {
    Long id;
    Long profileId;
    DocumentType docType;
    String docNumber;
    LocalDate issuedDate;
    String issuedPlace;
    LocalDate expiryDate;
    byte[] rawOcrData;
    Long frontFileId;
    Long backFileId;
    @Builder.Default
    DocumentStatus status = DocumentStatus.ACTIVE;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static IdentityDocument create(
            Long profileId,
            DocumentType docType,
            String docNumber,
            LocalDate issuedDate,
            String issuedPlace,
            Long frontFileId,
            Long backFileId
    ) {
        return IdentityDocument.builder()
                .profileId(profileId)
                .docType(docType)
                .docNumber(docNumber)
                .issuedDate(issuedDate)
                .issuedPlace(issuedPlace)
                .frontFileId(frontFileId)
                .backFileId(backFileId)
                .build();
    }

    public void setFrontFileId(Long frontFileId) {
        if (this.frontFileId.equals(frontFileId)) {
            return;
        }
        this.frontFileId = frontFileId;
        this.updatedAt = LocalDateTime.now();
    }

    public void setBackFileId(Long backFileId) {
        if (this.backFileId.equals(backFileId)) {
            return;
        }
        this.backFileId = backFileId;
        this.updatedAt = LocalDateTime.now();
    }
}
