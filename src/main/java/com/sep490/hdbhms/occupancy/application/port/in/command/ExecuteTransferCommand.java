package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExecuteTransferCommand(
        Long requestId,
        Long executedById,
        TransferHandoverData transferOutHandover,
        TransferHandoverData transferInHandover
) {
    public record TransferHandoverData(
            LocalDate handoverDate,
            String note,
            MeterReadingData electricity,
            MeterReadingData water,
            List<AssetData> assets
    ) {}

    public record MeterReadingData(
            BigDecimal currentValue,
            Long photoFileId,
            LocalDate readingDate
    ) {}

    public record AssetData(
            Long id,
            String assetName,
            String assetCategory,
            Integer quantity,
            AssetCondition currentCondition,
            String description,
            Long fileImageId
    ) {}
}
