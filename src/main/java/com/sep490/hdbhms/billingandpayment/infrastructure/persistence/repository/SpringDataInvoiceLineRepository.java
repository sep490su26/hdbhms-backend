package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.repository;

import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper.InvoiceLinePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataInvoiceLineRepository implements InvoiceLineRepository {
    JpaInvoiceLineRepository jpaInvoiceLineRepository;
    InvoiceLinePersistenceMapper invoiceLinePersistenceMapper;

    @Override
    public InvoiceLine save(InvoiceLine invoiceLine) {
        return invoiceLinePersistenceMapper.toDomain(
                jpaInvoiceLineRepository.save(
                        invoiceLinePersistenceMapper.toEntity(
                                invoiceLine
                        )
                )
        );
    }

    @Override
    public Optional<InvoiceLine> findById(Long id) {
        return jpaInvoiceLineRepository.findById(id)
                .map(invoiceLinePersistenceMapper::toDomain);
    }

    @Override
    public Optional<InvoiceLine> findByInvoiceId(Long invoiceId) {
        return jpaInvoiceLineRepository.findByInvoice_IdOrderByIdAsc(invoiceId)
                .stream()
                .findFirst()
                .map(invoiceLinePersistenceMapper::toDomain);
    }
}
