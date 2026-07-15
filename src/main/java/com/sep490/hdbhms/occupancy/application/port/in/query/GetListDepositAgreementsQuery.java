package com.sep490.hdbhms.occupancy.application.port.in.query;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public record GetListDepositAgreementsQuery(
        Long userId,
        DepositAgreementStatus status,
        List<DepositAgreementStatus> statuses,
        String search,
        Long floorId,
        LocalDateTime signedFrom,
        LocalDateTime signedTo,
        Pageable pageable
) {
}
