package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.property.domain.value_objects.AssetCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Complete handover payload. Electricity is required for move-out/transfer only. */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitHandoverRequest {
    @NotNull
    HandoverType handoverType;

    LocalDate handoverDate;
    String note;

    @Valid
    MeterInput electricity;

    @Valid
    List<AssetInput> assets;

    List<@NotNull @Positive Long> deletedAssetIds;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterInput {
        @NotNull
        @PositiveOrZero
        BigDecimal currentValue;

        Long photoFileId;
        LocalDate readingDate;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AssetInput {
        Long id;

        @NotBlank
        String assetName;

        @NotBlank
        String assetCategory;

        @NotNull
        @PositiveOrZero
        Integer quantity;

        @NotNull
        AssetCondition currentCondition;

        String description;
        Long fileImageId;

        @PositiveOrZero
        Long compensationAmount;

        @Size(max = 1000)
        String damageNote;

        Long evidenceFileId;
    }
}
