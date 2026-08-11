package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.AssetCondition;
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
 *  - electricity meter reading
 *  - room assets (upsert by id and soft-delete by deletedAssetIds)
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
    @NotNull(message = "Vui lòng nhập chỉ số điện")
    MeterInput electricity;

    @Valid
    List<AssetInput> assets;

    /** Existing room assets that should be soft-deleted with this submission. */
    List<@NotNull @Positive Long> deletedAssetIds;

    // ─────────────────────────────────────────────────────────────────────

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterInput {
        @NotNull(message = "Vui lòng nhập chỉ số điện")
        @PositiveOrZero(message = "Chỉ số điện không được âm")
        BigDecimal currentValue;

        /** File ID from /api/v1/files/upload – nullable */
        Long photoFileId;

        /** Ngày chụp ảnh / ngày ghi chỉ số */
        LocalDate readingDate;

        @NotNull(message = "Chỉ số nước là bắt buộc")
        @PositiveOrZero(message = "Chỉ số nước không được âm")
        BigDecimal waterValue;

        Long waterPhotoFileId;
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

        @NotNull(message = "Vui lòng nhập số lượng")
        @PositiveOrZero(message = "Số lượng không được âm")
        Integer quantity;

        @NotNull(message = "Vui lòng chọn tình trạng tài sản")
        AssetCondition currentCondition;

        String description;

        /** File ID from /api/v1/files/upload – nullable */
        Long fileImageId;

        @PositiveOrZero(message = "Khoản chênh lệch không được âm")
        Long compensationAmount;

        @Size(max = 1000, message = "Ghi chú thiệt hại không được vượt quá 1000 ký tự")
        String damageNote;

        Long evidenceFileId;
    }
}
