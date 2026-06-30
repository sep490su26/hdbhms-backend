package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import jakarta.validation.constraints.NotNull;

public record DepositAgreementStatusUpdateRequest(
        @NotNull(message = "Vui lòng chọn trạng thái cọc.")
        DepositAgreementStatus status
) {
}
