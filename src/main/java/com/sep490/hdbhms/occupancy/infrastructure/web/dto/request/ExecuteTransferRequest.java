package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.AssetCondition;
import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
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
        @Valid TransferHandoverPayload transferInHandover,
        SettlementType positiveDifferenceSettlementType,
        @PositiveOrZero(message = "Khoản bồi thường phòng cũ không được âm")
        Long oldRoomCompensationAmount,
        String oldRoomCompensationNote
) {
    public record TransferHandoverPayload(
            LocalDate handoverDate,
            String note,
            @Valid MeterReadingPayload electricity,
            @Valid List<AssetPayload> assets,
            @PositiveOrZero(message = "Incidental charge amount must not be negative")
            Long incidentalChargeAmount,
            String incidentalChargeNote
    ) {}

    public record MeterReadingPayload(
            @NotNull(message = "Meter reading value is required")
            @PositiveOrZero(message = "Chỉ số điện không được âm")
            BigDecimal currentValue,
            Long photoFileId,
            LocalDate readingDate,
            @NotNull(message = "Vui lòng nhập chỉ số nước")
            @PositiveOrZero(message = "Chỉ số nước không được nhỏ hơn 0")
            BigDecimal waterValue,
            Long waterPhotoFileId
    ) {}

    public record AssetPayload(
            Long id,
            @NotBlank(message = "Tên tài sản là bắt buộc")
            String assetName,
            @NotBlank(message = "Danh mục tài sản là bắt buộc")
            String assetCategory,
            @NotNull(message = "Số lượng tài sản là bắt buộc")
            @Min(value = 1, message = "Số lượng tài sản phải lớn hơn 0")
            Integer quantity,
            @NotNull(message = "Asset condition is required")
            AssetCondition currentCondition,
            String description,
            Long fileImageId
    ) {}
}
