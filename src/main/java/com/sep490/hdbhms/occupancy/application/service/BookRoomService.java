package com.sep490.hdbhms.occupancy.application.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookRoomService implements BookRoomUseCase {
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;
    DepositFormRepository depositFormRepository;
    UploadIdentityFilePort uploadIdentityFilePort;
    SendDepositPaymentPort sendDepositPaymentPort;
    CreateRoomHoldTaskPort createRoomHoldTaskPort;

    @Override
    public void initDepositForm(SendDepositFormCommand command) {
        if (!isRoomAvailableForBooking(command.roomId())) {
            throw new IllegalArgumentException(
                    "This room is currently unavailable for booking."
            );
        }
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
                    LocalDateTime.now().plusMinutes(15)
            );
            try {
                roomHold = roomHoldRepository.save(roomHold);
            } catch (DataIntegrityViolationException ex) {
                throw new RuntimeException("Room is already locked");
            }
            Room room = roomRepository.findById(roomHold.getRoomId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
            room.holdRoom();
            roomRepository.save(room);
            createRoomHoldTaskPort.execute(roomHold);
            sendDepositPaymentPort.execute(depositForm, roomHold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRoomAvailableForBooking(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (room.getCurrentStatus() != RoomStatus.VACANT) {
            return false;
        }
        boolean hasActiveHold = roomHoldRepository.existsByRoomIdAndStatusIn(
                roomId,
                List.of(RoomHoldStatus.ACTIVE, RoomHoldStatus.PAYMENT_PROCESSING)
        );
        return !hasActiveHold;
    }
}
