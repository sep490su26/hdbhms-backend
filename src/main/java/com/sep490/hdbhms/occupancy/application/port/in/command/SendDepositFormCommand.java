package com.sep490.hdbhms.occupancy.application.port.in.command;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public record SendDepositFormCommand(
        Long roomId,
        String fullName,
        LocalDate dob,
        String email,
        String phone,
        String permanentAddress,
        String idNumber,
        LocalDate idIssueDate,
        String idIssuePlace,
        Integer depositMonths,
        Integer paymentCycleMonths,
        Integer occupantCount,
        List<CoOccupant> coOccupants,
        LocalDate expectedMoveInDate,
        LocalDate expectedLeaseSignDate,
        MultipartFile idFrontFile,
        MultipartFile idBackFile,
        MultipartFile portraitFile
) {
    public record CoOccupant(
            String fullName,
            String phone,
            Integer displayOrder
    ) {
    }
}
