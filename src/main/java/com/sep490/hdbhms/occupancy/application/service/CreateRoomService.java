package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateRoomCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateRoomUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateRoomService implements CreateRoomUseCase {
    RoomRepository roomRepository;

    @Override
    public Room execute(CreateRoomCommand command) {
        String roomCode = nextAvailableRoomCode(command.propertyId(), command.roomCode());
        String roomName = command.name();
        if (roomName == null || roomName.isBlank() || roomName.equals(command.roomCode())) {
            roomName = roomCode;
        }
        Room room = Room.newRoom(
                command.propertyId(),
                command.floorId(),
                roomCode,
                roomName,
                command.areaM2(),
                command.listedPrice(),
                command.maxOccupants(),
                command.sortOrder()
        );
        return roomRepository.save(room);
    }

    private String nextAvailableRoomCode(Long propertyId, String requestedCode) {
        String code = requestedCode == null || requestedCode.isBlank() ? "1" : requestedCode.trim();
        while (roomRepository.existsActiveByPropertyIdAndRoomCode(propertyId, code)) {
            code = incrementCode(code);
        }
        return code;
    }

    private String incrementCode(String code) {
        int end = code.length();
        int start = end;
        while (start > 0 && Character.isDigit(code.charAt(start - 1))) {
            start--;
        }
        if (start == end) {
            return code + "1";
        }

        String prefix = code.substring(0, start);
        String digits = code.substring(start);
        long nextNumber = Long.parseLong(digits) + 1;
        return prefix + String.format("%0" + digits.length() + "d", nextNumber);
    }
}
