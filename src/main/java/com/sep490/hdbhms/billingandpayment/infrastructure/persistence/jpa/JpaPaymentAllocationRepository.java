package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface JpaPaymentAllocationRepository extends JpaRepository<PaymentAllocationEntity, Long> {
    @Query("""
            select max(allocation.paymentTransaction.transactionTime)
            from PaymentAllocationEntity allocation
            where allocation.invoice.id = :invoiceId
            """)
    Optional<Instant> findLatestTransactionTimeByInvoiceId(@Param("invoiceId") Long invoiceId);
}
