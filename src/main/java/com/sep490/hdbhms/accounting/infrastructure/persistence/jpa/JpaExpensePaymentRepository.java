package com.sep490.hdbhms.accounting.infrastructure.persistence.jpa;

import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpensePaymentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaExpensePaymentRepository extends JpaRepository<ExpensePaymentEntity, Long> {

    boolean existsByOperatingExpense_Id(Long operatingExpenseId);

    @EntityGraph(attributePaths = {"paidBy", "receiptFile"})
    Optional<ExpensePaymentEntity> findByOperatingExpense_Id(Long operatingExpenseId);
}
