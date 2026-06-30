package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositAgreementResponse {
    Long id;
    String depositCode;
    String roomCode;
    String propertyName;
    String depositorFullName;
    String depositorPhone;
    String depositorEmail;
    Long amount;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
    LocalDateTime createdAt;
    DepositAgreementStatus status;
    LocalDateTime confirmedAt;
    Long contractFileId;
    String contractDownloadUrl;
    String signatureStatus;
    String signatureStatusLabel;
    Long signedFileId;
    String signedFileName;
    LocalDateTime signedAt;
    Long signedUploadedById;
    String signedFileDownloadUrl;
    Boolean canPreviewDraft;
    Boolean canDownloadDraft;
    Boolean canUploadSignedFile;
    Boolean canViewSignedFile;
}
