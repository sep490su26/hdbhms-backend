package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitBatchMeterReadingsCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitSingleMeterReadingCommand;

public interface SubmitMeterReadingUseCase {
    void submitSingleReading(SubmitSingleMeterReadingCommand command);
    void submitBatchReadings(SubmitBatchMeterReadingsCommand command);
}
