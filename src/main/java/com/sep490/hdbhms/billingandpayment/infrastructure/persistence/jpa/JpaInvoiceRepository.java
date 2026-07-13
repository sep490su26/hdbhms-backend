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
    boolean existsByInvoiceCode(String invoiceCode);

    Optional<InvoiceEntity> findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(
            Long leaseContractId,
            String billingPeriod,
            InvoiceType invoiceType,
            InvoiceStatus status
    );

    Optional<InvoiceEntity> findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusNotOrderByIdDesc(
            Long leaseContractId,
            String billingPeriod,
            InvoiceType invoiceType,
            InvoiceStatus status
    );

    Optional<InvoiceEntity> findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndInvoiceReasonAndStatusNotOrderByIdDesc(
            Long leaseContractId,
            String billingPeriod,
            InvoiceType invoiceType,
            com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason invoiceReason,
            InvoiceStatus status
    );

    Optional<InvoiceEntity> findFirstByRoom_IdAndBillingPeriodAndInvoiceTypeAndStatusNotOrderByIdDesc(
            Long roomId,
            String billingPeriod,
            InvoiceType invoiceType,
            InvoiceStatus status
    );

    @Query("""
            SELECT DISTINCT invoice
            FROM InvoiceEntity invoice
            LEFT JOIN FETCH invoice.property property
            LEFT JOIN FETCH invoice.room room
            LEFT JOIN FETCH invoice.leastContract contract
            LEFT JOIN FETCH contract.primaryTenantProfile profile
            WHERE (:billingPeriod IS NULL OR invoice.billingPeriod = :billingPeriod)
              AND (:status IS NULL OR invoice.status = :status)
              AND (:propertyId IS NULL OR property.id = :propertyId)
              AND (:roomId IS NULL OR room.id = :roomId)
              AND (:invoiceType IS NULL OR invoice.invoiceType = :invoiceType)
            ORDER BY invoice.dueDate DESC, invoice.id DESC
            """)
    List<InvoiceEntity> findManagementInvoices(
            @Param("billingPeriod") String billingPeriod,
            @Param("status") InvoiceStatus status,
            @Param("propertyId") Long propertyId,
            @Param("roomId") Long roomId,
            @Param("invoiceType") InvoiceType invoiceType
    );

    @Query("""
            SELECT DISTINCT invoice
            FROM InvoiceEntity invoice
            LEFT JOIN FETCH invoice.property property
            LEFT JOIN FETCH invoice.room room
            LEFT JOIN FETCH invoice.leastContract contract
            LEFT JOIN FETCH contract.primaryTenantProfile profile
            WHERE invoice.remainingAmount > 0
              AND invoice.room IS NOT NULL
              AND invoice.invoiceType IN :invoiceTypes
              AND (:propertyId IS NULL OR property.id = :propertyId)
              AND (
                    invoice.status IN :statuses
                    OR (
                        invoice.status = com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus.ISSUED
                        AND invoice.dueDate < CURRENT_TIMESTAMP
                    )
              )
            ORDER BY property.name ASC, room.roomCode ASC, invoice.dueDate ASC
            """)
    List<InvoiceEntity> findDebtDashboardInvoices(
            @Param("propertyId") Long propertyId,
            @Param("statuses") Collection<InvoiceStatus> statuses,
            @Param("invoiceTypes") Collection<InvoiceType> invoiceTypes
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
