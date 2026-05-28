package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.DepositFormStatus;
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
public class DepositForm {
    final Long id;
    Long roomId;
    String idNumber;
    String fullName;
    String permanentAddress;
    LocalDate idIssueDate;
    String idIssuePlace;
    LocalDate dob;
    String email;
    String phone;
    Long idFrontFileId;
    Long idBackFileId;
    Long portraitFileId;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
    LocalDateTime paymentDueAt;
    LocalDate depositExpiresAt;

    @Builder.Default
    DepositFormStatus status = DepositFormStatus.APPROVAL_PENDING;
    LocalDateTime confirmedAt;
    String rejectReason;

    LocalDateTime createdAt;

    public static DepositForm newDepositForm(
            Long roomId,
            String fullName,
            LocalDate dob,
            String email,
            String phone,
            String permanentAddress,
            String idNumber,
            LocalDate idIssueDate,
            String idIssuePlace,
            Long idFrontFileId,
            Long idBackFileId,
            Long portraitFileId,
            LocalDate expectedMoveInDate,
            LocalDate expectedLeaseSignDate
    ) {
        return DepositForm.builder()
                .roomId(roomId)
                .dob(dob)
                .idNumber(idNumber)
                .fullName(fullName)
                .email(email)
                .idIssueDate(idIssueDate)
                .idIssuePlace(idIssuePlace)
                .idFrontFileId(idFrontFileId)
                .idBackFileId(idBackFileId)
                .portraitFileId(portraitFileId)
                .permanentAddress(permanentAddress)
                .phone(phone)
                .expectedMoveInDate(expectedMoveInDate)
                .expectedLeaseSignDate(expectedLeaseSignDate)
                .build();
    }

    public void approveDepositForm() {
        if (depositExpiresAt != null && depositExpiresAt.isBefore(LocalDate.now())) {
            throw new IllegalStateException("Deposit form is expired");
        }
        status = DepositFormStatus.APPROVED;
        confirmedAt = LocalDateTime.now();
    }
}
