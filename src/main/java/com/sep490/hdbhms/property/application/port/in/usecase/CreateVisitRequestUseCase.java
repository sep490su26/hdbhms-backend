package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.CreateVisitRequestCommand;
import com.sep490.hdbhms.property.domain.model.VisitRequest;

public interface CreateVisitRequestUseCase {
    VisitRequest execute(CreateVisitRequestCommand command);
}
