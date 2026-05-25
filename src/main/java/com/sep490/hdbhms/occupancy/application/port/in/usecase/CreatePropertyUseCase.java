package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.occupancy.domain.model.Property;

public interface CreatePropertyUseCase {
    Property execute(CreatePropertyCommand command);
}
