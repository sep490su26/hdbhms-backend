package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.AttachPropertyImageCommand;
import com.sep490.hdbhms.property.domain.model.PropertyImage;

public interface AttachPropertyImageUseCase {
    PropertyImage execute(AttachPropertyImageCommand command);
}
