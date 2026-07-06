package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceLineEntity;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceLineType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaInvoiceLineRepository extends JpaRepository<InvoiceLineEntity, Long> {
    List<InvoiceLineEntity> findByInvoice_IdOrderByIdAsc(Long invoiceId);

    Optional<InvoiceLineEntity> findFirstBySourceTypeAndSourceIdOrderByIdDesc(String sourceType, Long sourceId);

    Optional<InvoiceLineEntity> findFirstByInvoice_IdAndLineTypeOrderByIdAsc(Long invoiceId, InvoiceLineType lineType);
}
