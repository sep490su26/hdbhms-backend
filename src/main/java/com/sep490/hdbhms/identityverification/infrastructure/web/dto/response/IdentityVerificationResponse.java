package com.sep490.hdbhms.identityverification.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdentityVerificationResponse {
    boolean success;
    String message;
    String code;
    boolean qrExtracted;
    boolean ocrExtracted;
    String extractionMethod;
    String rawQrPayload;
    ExtractedIdentity extractedIdentity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExtractedIdentity {
        String idNumber;
        String fullName;
        LocalDate dob;
        String gender;
        String address;
        LocalDate issuedDate;
        String oldIdNumber;
    }
}
