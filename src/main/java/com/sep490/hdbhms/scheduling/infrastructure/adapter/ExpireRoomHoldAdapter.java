package com.sep490.hdbhms.scheduling.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.scheduling.application.port.out.ExpireRoomHoldPort;
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
    }
}
