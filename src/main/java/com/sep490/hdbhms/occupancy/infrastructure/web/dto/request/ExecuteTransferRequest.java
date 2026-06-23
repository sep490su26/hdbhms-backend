package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExecuteTransferRequest(
        @Valid TransferHandoverPayload transferOutHandover,
        @Valid TransferHandoverPayload transferInHandover
) {
    public record TransferHandoverPayload(
            LocalDate handoverDate,
            String note,
            @Valid MeterReadingPayload electricity,
            @Valid MeterReadingPayload water,
            @Valid List<AssetPayload> assets
    ) {}

    public record MeterReadingPayload(
            @NotNull(message = "Meter reading value is required")
            @PositiveOrZero(message = "Meter reading value must not be negative")
            BigDecimal currentValue,
            Long photoFileId,
            LocalDate readingDate
    ) {}

    public record AssetPayload(
            Long id,
            @NotBlank(message = "Asset name is required")
            String assetName,
            @NotBlank(message = "Asset category is required")
            String assetCategory,
            @NotNull(message = "Asset quantity is required")
            @Min(value = 1, message = "Asset quantity must be at least 1")
            Integer quantity,
            @NotNull(message = "Asset condition is required")
            AssetCondition currentCondition,
            String description,
            Long fileImageId
    ) {}
}
