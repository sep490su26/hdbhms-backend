package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.DepositFormCoOccupant;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookRoomService implements BookRoomUseCase {
    private static final long ROOM_HOLD_DURATION_MINUTES = 5;
    private static final int MAX_DEPOSIT_SCHEDULE_DAYS = 14;

    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    DepositFormRepository depositFormRepository;
    UploadIdentityFilePort uploadIdentityFilePort;
    SendDepositPaymentPort sendDepositPaymentPort;
    CreateRoomHoldTaskPort createRoomHoldTaskPort;
    RoomCommitmentChecker roomCommitmentChecker;

    @Override
    public PaymentIntent initDepositForm(SendDepositFormCommand command) {
        ensureRoomAvailableForBooking(command.roomId(), command.expectedMoveInDate(), command.expectedLeaseSignDate());
        try {
            FileMetadata idFrontFileMetadata = uploadIdentityFilePort.execute(command.idFrontFile(), FileCategory.ID_CARD);
            FileMetadata idBackFileMetadata = uploadIdentityFilePort.execute(command.idBackFile(), FileCategory.ID_CARD);
            FileMetadata portraitFileMetadata = uploadIdentityFilePort.execute(command.portraitFile(), FileCategory.ID_CARD);
            DepositForm depositForm = DepositForm.newDepositForm(
                    command.roomId(),
                    command.fullName(),
                    command.dob(),
                    command.email(),
                    command.phone(),
                    command.permanentAddress(),
                    command.idNumber(),
                    command.idIssueDate(),
                    command.idIssuePlace(),
                    command.depositMonths(),
                    command.paymentCycleMonths(),
                    command.occupantCount(),
                    toCoOccupants(command.coOccupants()),
                    idFrontFileMetadata.getId(),
                    idBackFileMetadata.getId(),
                    portraitFileMetadata.getId(),
                    command.expectedMoveInDate(),
                    command.expectedLeaseSignDate()
            );
            depositForm = depositFormRepository.save(depositForm);
            depositForm.approveDepositForm();
            depositFormRepository.save(depositForm);
            RoomHold roomHold = RoomHold.createRoomHoldForGuest(
                    depositForm.getRoomId(),
                    LocalDateTime.now().plusMinutes(ROOM_HOLD_DURATION_MINUTES)
            );
            try {
                roomHold = roomHoldRepository.save(roomHold);
            } catch (DataIntegrityViolationException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, buildActiveHoldMessage(depositForm.getRoomId()));
            }
            Room room = roomRepository.findById(roomHold.getRoomId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
            room.holdRoom();
            roomRepository.save(room);
            createRoomHoldTaskPort.execute(roomHold);
            return sendDepositPaymentPort.execute(depositForm, roomHold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRoomAvailableForBooking(Long roomId) {
        return getUnavailableReason(roomId, null, null) == null;
    }

    private void ensureRoomAvailableForBooking(Long roomId, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        String unavailableReason = getUnavailableReason(roomId, expectedMoveInDate, expectedLeaseSignDate);
        if (unavailableReason != null) {
            HttpStatus status = unavailableReason.startsWith("EXPECTED_")
                    ? HttpStatus.UNPROCESSABLE_ENTITY
                    : HttpStatus.CONFLICT;
            throw new ResponseStatusException(status, unavailableReason);
        }
    }

    private String getUnavailableReason(Long roomId, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();

        RoomHold activeHold = roomHoldRepository.findActiveHoldByRoomId(roomId, now).orElse(null);
        if (activeHold != null) {
            return buildActiveHoldMessage(activeHold, now);
        }

        if (room.getCurrentStatus() == RoomStatus.SOON_VACANT) {
            return validateSoonVacantBooking(roomId, expectedMoveInDate, expectedLeaseSignDate);
        }

        if (room.getCurrentStatus() != RoomStatus.VACANT) {
            if (room.getCurrentStatus() == RoomStatus.RESERVED) {
                return "Phòng đã được đặt cọc. Vui lòng chọn phòng khác.";
            }
            return "Phòng hiện không thể đặt cọc. Vui lòng chọn phòng khác.";
        }
        return validateRegularVacantBooking(expectedMoveInDate, expectedLeaseSignDate);
    }

    private String validateRegularVacantBooking(LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        LocalDate maxAllowedDate = LocalDate.now().plusDays(MAX_DEPOSIT_SCHEDULE_DAYS);
        if (expectedMoveInDate != null && expectedMoveInDate.isAfter(maxAllowedDate)) {
            return "EXPECTED_MOVE_IN_TOO_FAR: Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ hôm nay.";
        }
        if (expectedLeaseSignDate != null && expectedLeaseSignDate.isAfter(maxAllowedDate)) {
            return "EXPECTED_SIGN_DATE_TOO_FAR: Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ hôm nay.";
        }
        return null;
    }

    private String validateSoonVacantBooking(Long roomId, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        if (expectedLeaseSignDate == null) {
            return "EXPECTED_SIGN_DATE_REQUIRED: Can co ngay du kien ky hop dong.";
        }
        if (expectedMoveInDate == null) {
            return "EXPECTED_MOVE_IN_REQUIRED: Can co ngay du kien vao o.";
        }
        LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(roomId)
                .orElse(null);
        if (expectedVacantDate == null) {
            return "EXPECTED_VACANT_DATE_MISSING: Phong sap trong chua co ngay du kien ban giao.";
        }
        LocalDate minAllowedDate = expectedVacantDate.plusDays(1);
        LocalDate maxAllowedDate = expectedVacantDate.plusDays(MAX_DEPOSIT_SCHEDULE_DAYS);
        if (expectedMoveInDate.isBefore(minAllowedDate)) {
            return "EXPECTED_MOVE_IN_BEFORE_VACANT_DATE: Ngày dự kiến vào ở phải sau ngày khách cũ trả phòng.";
        }
        if (expectedLeaseSignDate.isBefore(minAllowedDate)) {
            return "EXPECTED_SIGN_DATE_BEFORE_VACANT_DATE: Ngày hẹn ký hợp đồng phải sau ngày khách cũ trả phòng.";
        }
        if (expectedMoveInDate.isAfter(maxAllowedDate)) {
            return "EXPECTED_MOVE_IN_TOO_FAR_AFTER_VACANT_DATE: Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ ngày khách cũ trả phòng.";
        }
        if (expectedLeaseSignDate.isAfter(maxAllowedDate)) {
            return "EXPECTED_SIGN_DATE_TOO_FAR_AFTER_VACANT_DATE: Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ ngày khách cũ trả phòng.";
        }
        return null;
    }

    private String buildActiveHoldMessage(Long roomId) {
        LocalDateTime now = LocalDateTime.now();
        return roomHoldRepository.findActiveHoldByRoomId(roomId, now)
                .map(roomHold -> buildActiveHoldMessage(roomHold, now))
                .orElse("Phòng đang có người đặt cọc. Vui lòng chờ trong giây lát.");
    }

    private String buildActiveHoldMessage(RoomHold roomHold, LocalDateTime now) {
        long remainingSeconds = Math.max(1, java.time.Duration.between(now, roomHold.getExpiresAt()).getSeconds());
        return "Phòng đang có người đặt cọc. Vui lòng chờ " + remainingSeconds + " giây.";
    }
    private List<DepositFormCoOccupant> toCoOccupants(List<SendDepositFormCommand.CoOccupant> coOccupants) {
        if (coOccupants == null) {
            return List.of();
        }
        return coOccupants.stream()
                .map(coOccupant -> DepositFormCoOccupant.builder()
                        .fullName(coOccupant.fullName())
                        .phone(coOccupant.phone())
                        .displayOrder(coOccupant.displayOrder())
                        .build())
                .toList();
    }
}
