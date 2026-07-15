package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.repository;

import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper.InvoicePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataInvoiceRepository implements InvoiceRepository {
    JpaInvoiceRepository jpaInvoiceRepository;
    InvoicePersistenceMapper invoicePersistenceMapper;

    @Override
    public Invoice save(Invoice invoice) {
        return invoicePersistenceMapper.toDomain(
                jpaInvoiceRepository.save(
                        invoicePersistenceMapper.toEntity(
                                invoice
                        )
                )
        );
    }

    @Override
    public Optional<Invoice> findById(Long id) {
        return jpaInvoiceRepository.findById(id)
                .map(invoicePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Invoice> findFirstByLeastContractIdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(Long leaseContractId, String billingPeriod, InvoiceType invoiceType, InvoiceStatus invoiceStatus) {
        return jpaInvoiceRepository.findFirstByLeastContract_IdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(leaseContractId, billingPeriod, invoiceType, invoiceStatus)
                .map(invoicePersistenceMapper::toDomain);
    }
}
