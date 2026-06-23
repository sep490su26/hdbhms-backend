package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PendingBillingChargeStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PendingBillingChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaPendingBillingChargeRepository extends JpaRepository<PendingBillingChargeEntity, Long> {
    List<PendingBillingChargeEntity> findByStatusAndScheduledIssueAtLessThanEqualOrderByContract_IdAscIdAsc(
            PendingBillingChargeStatus status,
            LocalDateTime scheduledIssueAt
    );

    Optional<PendingBillingChargeEntity> findFirstBySourceTypeAndSourceIdAndStatusInOrderByIdDesc(
            String sourceType,
            Long sourceId,
            Collection<PendingBillingChargeStatus> statuses
    );
}
