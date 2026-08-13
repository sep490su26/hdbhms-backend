package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.ReleaseRoomPort;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReleaseRoomAdapter implements ReleaseRoomPort {
    RoomRepository roomRepository;

    @Override
    public void execute(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow();
        room.releaseRoom(false);
        roomRepository.save(room);
    }

    @Override
    public void executeImmediately(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow();
        RoomStatus currentStatus = room.getCurrentStatus();
        if (currentStatus == RoomStatus.VACANT
                || currentStatus == RoomStatus.RESERVED
                || currentStatus == RoomStatus.RESERVED_FOR_TRANSFER
                || currentStatus == RoomStatus.ON_HOLD) {
            return;
        }
        room.releaseRoom(true);
        roomRepository.save(room);
    }
}
