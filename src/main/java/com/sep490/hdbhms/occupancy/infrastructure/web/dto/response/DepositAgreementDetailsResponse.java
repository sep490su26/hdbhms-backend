package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.DepositContactOutcome;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositAgreementDetailsResponse {
    Long id;
    String depositCode;
    RoomResponse room;
    String roomCode;
    String propertyName;
    String propertyAddress;
    Long floorId;
    String floorName;
    String depositorFullName;
    String depositorPhone;
    String depositorEmail;
    String depositorPermanentAddress;
    Long amount;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
    LocalDate depositExpiresAt;
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
    Long idFrontFileId;
    String idFrontFileUrl;
    Long idBackFileId;
    String idBackFileUrl;
    Long portraitFileId;
    String portraitFileUrl;
    String note;
    LocalDateTime createdAt;
    Integer extensionCount;
    Integer maxExtensions;
    LocalDate forfeitureDecisionDate;
    Long overdueDays;
    DepositContactOutcome latestContactOutcome;
    LocalDateTime lastContactedAt;
    String lastContactNote;
    Boolean contactRequired;
    Boolean canExtend;
    Boolean canForfeit;
}
