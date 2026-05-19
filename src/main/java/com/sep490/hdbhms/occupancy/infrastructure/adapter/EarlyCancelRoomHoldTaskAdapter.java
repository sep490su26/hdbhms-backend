package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.EarlyCancelRoomHoldTaskPort;
import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EarlyCancelRoomHoldTaskAdapter implements EarlyCancelRoomHoldTaskPort {
    ScheduledTaskRepository scheduledTaskRepository;

    @Override
    public void execute(Long roomHoldId) {
        scheduledTaskRepository.cancelForTarget("RoomHold", roomHoldId);
    }
}
