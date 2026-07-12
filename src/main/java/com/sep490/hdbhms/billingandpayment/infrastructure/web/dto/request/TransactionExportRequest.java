package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

import java.time.LocalDate;

public record TransactionExportRequest(
        Long roomId,
        String tenantName,
        LocalDate fromDate,
        LocalDate toDate,
        String format
) {
}
