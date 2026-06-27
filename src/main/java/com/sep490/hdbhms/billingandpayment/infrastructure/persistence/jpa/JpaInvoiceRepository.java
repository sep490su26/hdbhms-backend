package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaInvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(
            Long leaseContractId,
            String billingPeriod,
            InvoiceType invoiceType,
            InvoiceStatus status
    );

    @Query("""
            SELECT DISTINCT invoice
            FROM InvoiceEntity invoice
            LEFT JOIN FETCH invoice.room room
            LEFT JOIN FETCH room.property property
            LEFT JOIN FETCH invoice.leastContract contract
            LEFT JOIN contract.primaryTenantProfile primaryProfile
            WHERE invoice.status IN :statuses
              AND contract IS NOT NULL
              AND (
                    primaryProfile.user.id = :userId
                    OR EXISTS (
                        SELECT occupant.id
                        FROM ContractOccupantEntity occupant
                        WHERE occupant.contract = contract
                          AND occupant.status = com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus.ACTIVE
                          AND occupant.tenantProfile.user.id = :userId
                    )
              )
            ORDER BY invoice.dueDate ASC, invoice.id DESC
            """)
    List<InvoiceEntity> findTenantVisibleInvoices(
            @Param("userId") Long userId,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );
}
