package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.AttachRoomImageCommand;
import com.sep490.hdbhms.property.domain.model.RoomImage;

public interface AttachRoomImageUseCase {
    RoomImage execute(AttachRoomImageCommand command);
}
