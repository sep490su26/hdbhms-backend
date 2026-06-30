package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileMetadataResponse;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonProfileResponse {
    Long id;
    Long userId;
    String fullName;
    LocalDate dob;
    Gender gender;
    String phone;
    String email;
    String permanentAddress;
    FileMetadataResponse portraitFile;
    IdentityDocumentResponse identityDocument;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IdentityDocumentResponse {
        Long id;
        String docType;
        String docNumber;
        LocalDate issuedDate;
        String issuedPlace;
        LocalDate expiryDate;
        Long frontFileId;
        Long backFileId;
        String frontFileUrl;
        String backFileUrl;
        String status;
    }
}
