package com.sep490.hdbhms.booking.application.service;

import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.booking.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.booking.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.booking.application.port.out.CreateRoomHoldTaskPort;
import com.sep490.hdbhms.booking.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.booking.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.booking.application.port.out.SendDepositPaymentPort;
import com.sep490.hdbhms.occupancy.application.port.out.UploadIdentityFilePort;
import com.sep490.hdbhms.booking.domain.model.DepositForm;
import com.sep490.hdbhms.booking.domain.model.DepositFormCoOccupant;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.booking.domain.model.RoomHold;
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
    private static final String ROOM_DEPOSIT_LOCKED_PREFIX = "ROOM_DEPOSIT_LOCKED: ";

    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    DepositFormRepository depositFormRepository;
    UploadIdentityFilePort uploadIdentityFilePort;
    SendDepositPaymentPort sendDepositPaymentPort;
    CreateRoomHoldTaskPort createRoomHoldTaskPort;
    RoomCommitmentChecker roomCommitmentChecker;
    RoomDepositLockService roomDepositLockService;

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
                    Gender.fromLabel(command.gender()),
                    command.email(),
                    command.phone(),
                    command.permanentAddress(),
                    command.idNumber(),
                    command.idIssueDate(),
                    command.idIssuePlace(),
                    command.depositMonths(),
                    command.contractTermMonths(),
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
//            try {
//                roomHold = roomHoldRepository.save(roomHold);
//            } catch (DataIntegrityViolationException ex) {
//                throw new AppException(ApiErrorCode.ROOM_DEPOSIT_HOLD_ACTIVE);
//            }
            Room room = roomRepository.findById(depositForm.getRoomId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
//            room.holdRoom();
            roomRepository.save(room);
//            createRoomHoldTaskPort.execute(roomHold);
            return sendDepositPaymentPort.execute(depositForm, roomHold);
        } catch (IOException e) {
            throw new AppException(ApiErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }

    private void ensureRoomAvailableForBooking(Long roomId, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        String unavailableReason = getUnavailableReason(roomId, expectedMoveInDate, expectedLeaseSignDate);
        if (unavailableReason != null) {
            boolean locked = unavailableReason.startsWith(ROOM_DEPOSIT_LOCKED_PREFIX);
            HttpStatus status = HttpStatus.CONFLICT;
            if (locked) {
                status = HttpStatus.LOCKED;
            } else if (unavailableReason.startsWith("EXPECTED_")) {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
            }
            String message = locked
                    ? unavailableReason.substring(ROOM_DEPOSIT_LOCKED_PREFIX.length())
                    : unavailableReason;
            throw new AppException(unavailableCode(unavailableReason));
        }
    }

    private ApiErrorCode unavailableCode(String reason) {
        if (reason.startsWith(ROOM_DEPOSIT_LOCKED_PREFIX)) {
            return ApiErrorCode.ROOM_DEPOSIT_LOCKED;
        }
        if (reason.startsWith("EXPECTED_MOVE_IN_BEFORE_SIGN_DATE")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_MOVE_IN_BEFORE_SIGN_DATE;
        }
        if (reason.startsWith("EXPECTED_MOVE_IN_TOO_FAR_AFTER_VACANT")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_MOVE_IN_TOO_FAR_AFTER_VACANT;
        }
        if (reason.startsWith("EXPECTED_SIGN_DATE_TOO_FAR_AFTER_VACANT")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_SIGN_DATE_TOO_FAR_AFTER_VACANT;
        }
        if (reason.startsWith("EXPECTED_MOVE_IN_TOO_FAR")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_MOVE_IN_TOO_FAR;
        }
        if (reason.startsWith("EXPECTED_SIGN_DATE_TOO_FAR")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_SIGN_DATE_TOO_FAR;
        }
        if (reason.startsWith("EXPECTED_SIGN_DATE_REQUIRED")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_SIGN_DATE_REQUIRED;
        }
        if (reason.startsWith("EXPECTED_MOVE_IN_REQUIRED")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_MOVE_IN_REQUIRED;
        }
        if (reason.startsWith("EXPECTED_VACANT_DATE_MISSING")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_VACANT_DATE_MISSING;
        }
        if (reason.startsWith("EXPECTED_SIGN_DATE_BEFORE_VACANT_DATE")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_SIGN_DATE_BEFORE_VACANT_DATE;
        }
        if (reason.startsWith("EXPECTED_MOVE_IN_BEFORE_VACANT_DATE")) {
            return ApiErrorCode.DEPOSIT_EXPECTED_MOVE_IN_BEFORE_VACANT_DATE;
        }
        if (reason.startsWith("Phòng đang có người đặt cọc")) {
            return ApiErrorCode.ROOM_DEPOSIT_HOLD_ACTIVE;
        }
        if (reason.startsWith("Phòng đã được đặt cọc")) {
            return ApiErrorCode.ROOM_ALREADY_RESERVED;
        }
        return ApiErrorCode.ROOM_DEPOSIT_UNAVAILABLE;
    }

    private String getUnavailableReason(Long roomId, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();

        RoomDepositLockService.RoomDepositLock lock = roomDepositLockService.getActiveLock(roomId).orElse(null);
        if (lock != null) {
            return ROOM_DEPOSIT_LOCKED_PREFIX + roomDepositLockService.buildLockMessage(lock.remainingSeconds());
        }

        RoomHold activeHold = roomHoldRepository.findActiveHoldByRoomId(roomId, now).orElse(null);
        if (activeHold != null) {
            return buildActiveHoldMessage(activeHold, now);
        }

        return switch (room.getCurrentStatus()) {
            case SOON_VACANT -> validateSoonVacantBooking(roomId, expectedMoveInDate, expectedLeaseSignDate);
            case VACANT -> validateRegularVacantBooking(expectedMoveInDate, expectedLeaseSignDate);
            case RESERVED -> "Phòng đã được đặt cọc. Vui lòng chọn phòng khác.";
            default -> "Phòng hiện không thể đặt cọc. Vui lòng chọn phòng khác.";
        };
    }

    private String validateExpectedDateOrder(LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        if (expectedMoveInDate != null
                && expectedLeaseSignDate != null
                && expectedMoveInDate.isBefore(expectedLeaseSignDate)) {
            return "EXPECTED_MOVE_IN_BEFORE_SIGN_DATE: Ngày dự kiến vào ở không được trước ngày hẹn ký hợp đồng.";
        }
        return null;
    }

    private String validateRegularVacantBooking(LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        LocalDate maxAllowedDate = LocalDate.now().plusDays(MAX_DEPOSIT_SCHEDULE_DAYS);
        if (expectedMoveInDate != null && expectedMoveInDate.isAfter(maxAllowedDate)) {
            return "EXPECTED_MOVE_IN_TOO_FAR: Ngày dự kiến vào ở chỉ được tối đa 14 ngày kể từ hôm nay.";
        }
        if (expectedLeaseSignDate != null && expectedLeaseSignDate.isAfter(maxAllowedDate)) {
            return "EXPECTED_SIGN_DATE_TOO_FAR: Ngày hẹn ký hợp đồng chỉ được tối đa 14 ngày kể từ hôm nay.";
        }
        return validateExpectedDateOrder(expectedMoveInDate, expectedLeaseSignDate);
    }

    private String validateSoonVacantBooking(Long roomId, LocalDate expectedMoveInDate, LocalDate expectedLeaseSignDate) {
        if (expectedLeaseSignDate == null) {
            return "EXPECTED_SIGN_DATE_REQUIRED: Cần có ngày dự kiến ký hợp đồng.";
        }
        if (expectedMoveInDate == null) {
            return "EXPECTED_MOVE_IN_REQUIRED: Cần có ngày dự kiến vào ở.";
        }
        LocalDate expectedVacantDate = roomCommitmentChecker.findExpectedVacantDateForBooking(roomId)
                .orElse(null);
        if (expectedVacantDate == null) {
            return "EXPECTED_VACANT_DATE_MISSING: Phòng sắp trống chưa có ngày dự kiến bàn giao.";
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
        return validateExpectedDateOrder(expectedMoveInDate, expectedLeaseSignDate);
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
