package com.sep490.hdbhms.portal.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomeResponse {
    UserHomeResponse user;
    TenantHomeResponse tenant;
    RoomHomeResponse room;
    ContractHomeResponse contract;
    InvoiceSummaryHomeResponse invoiceSummary;
    UtilitySummaryHomeResponse utilitySummary;
}
