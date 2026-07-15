package com.sep490.hdbhms.accounting.infrastructure.persistence.jpa;

import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpenseApprovalRequestEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaExpenseApprovalRequestRepository extends JpaRepository<ExpenseApprovalRequestEntity, Long> {

    @EntityGraph(attributePaths = {"operatingExpense", "changeRequest"})
    Optional<ExpenseApprovalRequestEntity> findByOperatingExpense_Id(Long operatingExpenseId);

    @EntityGraph(attributePaths = {"operatingExpense", "changeRequest"})
    Optional<ExpenseApprovalRequestEntity> findByChangeRequest_Id(Long changeRequestId);
}
