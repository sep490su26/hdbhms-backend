package com.sep490.hdbhms.accounting.infrastructure.persistence.jpa;

import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpenseAttachmentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaExpenseAttachmentRepository extends JpaRepository<ExpenseAttachmentEntity, Long> {

    @EntityGraph(attributePaths = {"file"})
    List<ExpenseAttachmentEntity> findAllByOperatingExpense_IdOrderByIdAsc(Long operatingExpenseId);
}
