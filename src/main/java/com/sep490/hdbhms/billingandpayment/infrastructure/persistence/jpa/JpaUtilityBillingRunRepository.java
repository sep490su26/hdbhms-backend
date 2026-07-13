package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.UtilityBillingRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUtilityBillingRunRepository extends JpaRepository<UtilityBillingRunEntity, Long> {
    Optional<UtilityBillingRunEntity> findByProperty_IdAndBillingPeriodAndInvoiceReason(
            Long propertyId,
            String billingPeriod,
            InvoiceReason invoiceReason
    );

    List<UtilityBillingRunEntity> findByProperty_IdOrderByBillingPeriodDescIdDesc(Long propertyId);

    boolean existsByProperty_IdAndBillingPeriodAndInvoiceReasonAndStatusNot(
            Long propertyId,
            String billingPeriod,
            InvoiceReason invoiceReason,
            UtilityBillingRunStatus status
    );
}
