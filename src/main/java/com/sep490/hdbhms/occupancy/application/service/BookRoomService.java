package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
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
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookRoomService implements BookRoomUseCase {
    private static final long ROOM_HOLD_DURATION_MINUTES = 5;

    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    DepositFormRepository depositFormRepository;
    UploadIdentityFilePort uploadIdentityFilePort;
    SendDepositPaymentPort sendDepositPaymentPort;
    CreateRoomHoldTaskPort createRoomHoldTaskPort;

    @Override
    public PaymentIntent initDepositForm(SendDepositFormCommand command) {
        ensureRoomAvailableForBooking(command.roomId());
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
                    .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
            room.holdRoom();
            roomRepository.save(room);
            createRoomHoldTaskPort.execute(roomHold);
            return sendDepositPaymentPort.execute(depositForm, roomHold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRoomAvailableForBooking(Long roomId) {
        return getUnavailableReason(roomId) == null;
    }

    private void ensureRoomAvailableForBooking(Long roomId) {
        String unavailableReason = getUnavailableReason(roomId);
        if (unavailableReason != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, unavailableReason);
        }
    }

    private String getUnavailableReason(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        LocalDateTime now = LocalDateTime.now();

        RoomHold activeHold = roomHoldRepository.findActiveHoldByRoomId(roomId, now).orElse(null);
        if (activeHold != null) {
            return buildActiveHoldMessage(activeHold, now);
        }

        if (room.getCurrentStatus() != RoomStatus.VACANT) {
            if (room.getCurrentStatus() == RoomStatus.RESERVED) {
                return "Phòng đã được đặt cọc. Vui lòng chọn phòng khác.";
            }
            return "Phòng hiện không thể đặt cọc. Vui lòng chọn phòng khác.";
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
}
