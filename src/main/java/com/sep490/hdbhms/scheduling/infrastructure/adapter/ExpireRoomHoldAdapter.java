package com.sep490.hdbhms.scheduling.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.scheduling.application.port.out.ExpireRoomHoldPort;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpireRoomHoldAdapter implements ExpireRoomHoldPort {
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;

    @Override
    public void execute(Long roomHoldId) {
        RoomHold roomHold = roomHoldRepository.findById(roomHoldId)
                .orElse(null);
        if (roomHold == null) {
            return;
        }
        roomHold.releaseOnAutoExpired();
        roomHoldRepository.save(roomHold);
        Room room = roomRepository.findById(roomHold.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        room.releaseRoom();
        roomRepository.save(room);
    }
}
