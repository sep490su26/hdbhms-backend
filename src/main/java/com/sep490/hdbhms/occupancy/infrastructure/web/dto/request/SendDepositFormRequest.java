package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendDepositFormRequest {
    Long roomId;
    String fullName;
    LocalDate dob;
    String email;
    String phone;
    String permanentAddress;
    String idNumber;
    String idIssueDate;
    String idIssuePlace;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
}
