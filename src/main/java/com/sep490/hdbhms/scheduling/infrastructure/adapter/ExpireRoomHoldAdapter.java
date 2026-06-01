package com.sep490.hdbhms.scheduling.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.scheduling.application.port.out.ExpireRoomHoldPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpireRoomHoldAdapter implements ExpireRoomHoldPort {
    RoomRepository roomRepository;
    RoomHoldRepository roomHoldRepository;

    @Override
    public void execute(Long roomHoldId) {
        RoomHold roomHold = roomHoldRepository.findById(roomHoldId).orElse(null);
        if (roomHold == null || roomHold.getStatus() != RoomHoldStatus.ACTIVE) {
            return;
        }

        // Release the hold (idempotent)
        roomHold.releaseOnAutoExpired();
        roomHoldRepository.save(roomHold);

        int updatedRows = roomRepository.updateRoomStatusIfCurrent(
                roomHold.getRoomId(),
                RoomStatus.ON_HOLD,
                RoomStatus.VACANT
        );
        if (updatedRows == 0) {
            log.info("Skip releasing room hold because room status changed. roomHoldId={}, roomId={}",
                    roomHoldId,
                    roomHold.getRoomId());
        }
    }
}
