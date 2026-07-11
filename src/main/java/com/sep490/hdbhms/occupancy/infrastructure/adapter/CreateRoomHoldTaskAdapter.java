package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.occupancy.application.port.out.CreateRoomHoldTaskPort;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateRoomHoldTaskAdapter implements CreateRoomHoldTaskPort {
    ScheduledTaskRepository scheduledTaskRepository;

    @Override
    public void execute(RoomHold roomHold) {
        ScheduledTask scheduledTask = ScheduledTask.create(
                TaskType.ROOM_HOLD_EXPIRATION,
                "RoomHold",
                roomHold.getId(),
                roomHold.getExpiresAt()
        );
        scheduledTaskRepository.save(scheduledTask);
    }
}
