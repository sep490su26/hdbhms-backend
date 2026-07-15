package com.sep490.hdbhms.accounting.infrastructure.persistence.jpa;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseStatus;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.OperatingExpenseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface JpaOperatingExpenseRepository extends JpaRepository<OperatingExpenseEntity, Long> {

    @EntityGraph(attributePaths = {"property", "room", "createdBy", "approvedBy", "paidByUser", "receiptFile"})
    @Query("""
            SELECT e
            FROM OperatingExpenseEntity e
            JOIN e.property p
            LEFT JOIN e.room r
            WHERE (:propertyId IS NULL OR p.id = :propertyId)
              AND (:roomId IS NULL OR r.id = :roomId)
              AND (:expenseType IS NULL OR e.expenseType = :expenseType)
              AND (:status IS NULL OR e.status = :status)
              AND (:fromDate IS NULL OR e.expenseDate >= :fromDate)
              AND (:toDate IS NULL OR e.expenseDate <= :toDate)
              AND (
                    :keyword IS NULL
                    OR LOWER(e.expenseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(r.roomCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<OperatingExpenseEntity> findFiltered(
            @Param("propertyId") Long propertyId,
            @Param("roomId") Long roomId,
            @Param("expenseType") ExpenseType expenseType,
            @Param("status") ExpenseStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
