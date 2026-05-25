package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record SendDepositFormCommand(Long roomId,
                                     String fullName,
                                     LocalDate dob,
                                     String email,
                                     String phone,
                                     String permanentAddress,
                                     String idNumber,
                                     LocalDate idIssueDate,
                                     String idIssuePlace,
                                     LocalDate expectedMoveInDate,
                                     LocalDate expectedLeaseSignDate) {
}
