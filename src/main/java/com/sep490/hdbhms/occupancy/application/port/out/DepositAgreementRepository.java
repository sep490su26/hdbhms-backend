package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DepositAgreementRepository {
    DepositAgreement save(DepositAgreement depositAgreement);

    Optional<DepositAgreement> findById(Long id);

    List<DepositAgreement> findAll();

    Page<DepositAgreement> findAll(
            List<Long> ids,
            DepositAgreementStatus status,
            List<DepositAgreementStatus> statuses,
            String search,
            Long floorId,
            LocalDateTime signedFrom,
            LocalDateTime signedTo,
            Pageable pageable
    );

    default long countByStatuses(List<Long> ids, List<DepositAgreementStatus> statuses) {
        return 0;
    }

    default long sumAmountByStatuses(List<Long> ids, List<DepositAgreementStatus> statuses) {
        return 0;
    }

    default List<Long> findDistinctFloorIds(List<Long> ids, List<DepositAgreementStatus> statuses) {
        return List.of();
    }

    default boolean existsByDepositCode(String depositCode) {
        return false;
    }

    List<DepositAgreement> findAllByTenantId(Long tenantId);

    List<DepositAgreement> findAllAccessibleByUserId(Long userId);
}
