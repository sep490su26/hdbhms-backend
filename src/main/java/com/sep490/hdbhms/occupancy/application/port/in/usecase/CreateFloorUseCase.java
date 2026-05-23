package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateFloorCommand;
import com.sep490.hdbhms.occupancy.domain.model.Floor;

public interface CreateFloorUseCase {
    Floor execute(CreateFloorCommand command);
}
