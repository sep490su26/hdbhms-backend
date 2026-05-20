package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.BookRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public void initDepositForm(SendDepositFormCommand command) {
        if (!isRoomAvailableForBooking(command.roomId())) {
            throw new IllegalArgumentException(
                    "This room is currently unavailable for booking."
            );
        }
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
                command.expectedMoveInDate(),
                command.expectedLeaseSignDate()
        );
        log.info(depositForm.toString());
        depositFormRepository.save(depositForm);
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
