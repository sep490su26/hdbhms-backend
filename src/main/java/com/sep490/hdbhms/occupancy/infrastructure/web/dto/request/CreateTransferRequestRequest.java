package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateTransferRequestRequest(
        @NotNull(message = "Vui lòng chọn hợp đồng")
        Long sourceContractId,

        @NotNull(message = "Vui lòng chọn phòng muốn chuyển đến")
        Long targetRoomId,

        LocalDate requestedTransferDate,

        @NotNull(message = "Vui lòng chọn tháng chuyển dự kiến")
        LocalDate expectedTransferDate,

        List<Long> transferredTenantProfileIds,

        String reason
) {
    public CreateTransferRequestRequest {
        LocalDate transferMonth = expectedTransferDate != null
                ? expectedTransferDate
                : requestedTransferDate;
        transferMonth = transferMonth == null ? null : transferMonth.withDayOfMonth(1);
        requestedTransferDate = transferMonth;
        expectedTransferDate = transferMonth;
    }
}
