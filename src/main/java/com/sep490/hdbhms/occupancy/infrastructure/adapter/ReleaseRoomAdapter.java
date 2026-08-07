package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.ReleaseRoomPort;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.domain.model.Room;
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
}
