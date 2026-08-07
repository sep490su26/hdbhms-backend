package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.DeleteRoomImageCommand;

public interface DeleteRoomImageUseCase {
    void execute(DeleteRoomImageCommand command);
}
