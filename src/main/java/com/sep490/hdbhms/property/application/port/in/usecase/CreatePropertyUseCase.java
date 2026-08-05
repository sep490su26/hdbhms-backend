package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.property.domain.model.Property;

public interface CreatePropertyUseCase {
    Property execute(CreatePropertyCommand command);
}
