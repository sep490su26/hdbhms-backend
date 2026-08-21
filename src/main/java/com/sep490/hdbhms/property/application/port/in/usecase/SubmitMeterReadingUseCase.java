package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.command.SubmitBatchMeterReadingsCommand;
import com.sep490.hdbhms.property.application.port.in.command.SubmitSingleMeterReadingCommand;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.MeterReadingExcelImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SubmitMeterReadingUseCase {
    void submitSingleReading(SubmitSingleMeterReadingCommand command);
    void submitBatchReadings(SubmitBatchMeterReadingsCommand command);

    Long startBatch(String period, Long propertyId);
    void saveProgressiveRoomReading(Long batchId, Long roomId, java.math.BigDecimal electricityValue,
                                    Long elecPhotoId);
    MeterReadingExcelImportResponse importExcel(Long batchId, MultipartFile file);
    void resolveRoomReadingAnomalies(Long batchId, Long roomId);
    void confirmBatch(Long batchId);
}
