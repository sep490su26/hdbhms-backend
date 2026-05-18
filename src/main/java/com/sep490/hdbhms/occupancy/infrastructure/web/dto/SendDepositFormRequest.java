package com.sep490.hdbhms.occupancy.infrastructure.web.dto;

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
    String idNumber;
    String fullName;
    String email;
    String phone;
    LocalDate expectedMoveInDate;
    LocalDate expectedLeaseSignDate;
}
