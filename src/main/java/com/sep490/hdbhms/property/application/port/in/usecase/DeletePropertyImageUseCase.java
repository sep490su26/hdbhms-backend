package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.DeletePropertyImageCommand;

public interface DeletePropertyImageUseCase {
    void execute(DeletePropertyImageCommand command);
}
