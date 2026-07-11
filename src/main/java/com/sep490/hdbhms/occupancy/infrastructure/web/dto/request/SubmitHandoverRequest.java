package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Single-shot request that creates or updates a complete handover record:
 *  - meter readings (electricity + water)
 *  - room assets (upsert by id)
 *  - the ContractHandoverRecord itself (CONFIRMED on success)
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitHandoverRequest {

    /** CHECK_IN or CHECK_OUT */
    @NotNull(message = "Loại bàn giao là bắt buộc")
    HandoverType handoverType;

    /** Date the handover physically happens */
    LocalDate handoverDate;

    String note;

    @Valid
    @NotNull(message = "Chỉ số điện là bắt buộc")
    MeterInput electricity;

    @Valid
    @NotNull(message = "Chỉ số nước là bắt buộc")
    MeterInput water;

    @Valid
    List<AssetInput> assets;

    // ─────────────────────────────────────────────────────────────────────

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterInput {
        @NotNull(message = "Chỉ số là bắt buộc")
        @PositiveOrZero(message = "Chỉ số không được âm")
        BigDecimal currentValue;

        /** File ID from /api/v1/files/upload – nullable */
        Long photoFileId;

        /** Ngày chụp ảnh / ngày ghi chỉ số */
        LocalDate readingDate;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AssetInput {
        /** null → create new; non-null → update existing */
        Long id;

        @NotBlank(message = "Tên thiết bị là bắt buộc")
        String assetName;

        @NotBlank(message = "Danh mục thiết bị là bắt buộc")
        String assetCategory;

        @NotNull(message = "Số lượng là bắt buộc")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        Integer quantity;

        @NotNull(message = "Tình trạng là bắt buộc")
        AssetCondition currentCondition;

        String description;

        /** File ID from /api/v1/files/upload – nullable */
        Long fileImageId;
    }
}
