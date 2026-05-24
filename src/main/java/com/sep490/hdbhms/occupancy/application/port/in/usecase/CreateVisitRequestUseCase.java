package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateVisitRequestCommand;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;

public interface CreateVisitRequestUseCase {
    VisitRequest execute(CreateVisitRequestCommand command);
}
