package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.CreateRoomCommand;
import com.sep490.hdbhms.property.domain.model.Room;

public interface CreateRoomUseCase {
    Room execute(CreateRoomCommand command);
}
