package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.CreateFloorCommand;
import com.sep490.hdbhms.property.domain.model.Floor;

public interface CreateFloorUseCase {
    Floor execute(CreateFloorCommand command);
}
