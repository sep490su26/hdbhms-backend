package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositAgreementSignedFileResponse {
    Long depositAgreementId;
    String depositCode;
    String signatureStatus;
    Long signedFileId;
    String signedFileName;
    LocalDateTime signedAt;
    String message;
}
