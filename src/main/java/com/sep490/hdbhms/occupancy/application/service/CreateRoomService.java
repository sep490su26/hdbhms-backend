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
        Room room = Room.newRoom(
                command.propertyId(),
                command.floorId(),
                command.roomCode(),
                command.name(),
                command.areaM2(),
                command.listedPrice(),
                command.maxOccupants(),
                command.sortOrder()
        );
        return roomRepository.save(room);
    }
}
