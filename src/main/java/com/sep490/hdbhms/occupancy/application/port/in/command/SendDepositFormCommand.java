package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record SendDepositFormCommand(Long roomId,
                                     String idNumber,
                                     String fullName,
                                     String email,
                                     String phone,
                                     LocalDate expectedMoveInDate,
                                     LocalDate expectedLeaseSignDate) {
}
