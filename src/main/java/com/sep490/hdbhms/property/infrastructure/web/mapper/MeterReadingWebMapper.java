package com.sep490.hdbhms.property.infrastructure.web.mapper;

import com.sep490.hdbhms.property.application.port.in.command.SubmitBatchMeterReadingsCommand;
import com.sep490.hdbhms.property.application.port.in.command.SubmitSingleMeterReadingCommand;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.BatchMeterReadingRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.SingleMeterReadingRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MeterReadingWebMapper {
    SubmitSingleMeterReadingCommand toCommand(SingleMeterReadingRequest request);
    SubmitBatchMeterReadingsCommand toCommand(BatchMeterReadingRequest request);
}
