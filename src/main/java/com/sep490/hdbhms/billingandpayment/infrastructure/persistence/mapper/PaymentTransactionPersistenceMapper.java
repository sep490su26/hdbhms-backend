package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentTransaction;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaCollectionAccountRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentTransactionPersistenceMapper {
    JpaCollectionAccountRepository collectionAccountRepository;
    JpaUserRepository userRepository;

    public PaymentTransaction toDomain(PaymentTransactionEntity entity) {
        if (entity == null) return null;
        Long collectionAccountId = entity.getCollectionAccount() != null ? entity.getCollectionAccount().getId() : null;
        Long confirmedBy = entity.getConfirmedBy() != null ? entity.getConfirmedBy().getId() : null;
        
        return PaymentTransaction.builder()
                .id(entity.getId())
                .provider(entity.getProvider())
                .providerTransactionId(entity.getProviderTransactionId())
                .collectionAccountId(collectionAccountId)
                .amount(entity.getAmount())
                .transactionTime(entity.getTransactionTime() != null ? LocalDateTime.ofInstant(entity.getTransactionTime(), ZoneId.systemDefault()) : null)
                .payerName(entity.getPayerName())
                .payerAccount(entity.getPayerAccount())
                .content(entity.getContent())
                .status(entity.getStatus())
                .rawPayload(entity.getRawPayload())
                .confirmedBy(confirmedBy)
                .confirmedAt(entity.getConfirmedAt() != null ? LocalDateTime.ofInstant(entity.getConfirmedAt(), ZoneId.systemDefault()) : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PaymentTransactionEntity toEntity(PaymentTransaction domain) {
        if (domain == null) return null;
        var collectionAccount = domain.getCollectionAccountId() != null 
                ? collectionAccountRepository.findById(domain.getCollectionAccountId()).orElse(null) : null;
        var confirmedBy = domain.getConfirmedBy() != null 
                ? userRepository.findById(domain.getConfirmedBy()).orElse(null) : null;

        return PaymentTransactionEntity.builder()
                .id(domain.getId())
                .provider(domain.getProvider())
                .providerTransactionId(domain.getProviderTransactionId())
                .collectionAccount(collectionAccount)
                .amount(domain.getAmount())
                .transactionTime(domain.getTransactionTime() != null ? domain.getTransactionTime().atZone(ZoneId.systemDefault()).toInstant() : null)
                .payerName(domain.getPayerName())
                .payerAccount(domain.getPayerAccount())
                .content(domain.getContent())
                .status(domain.getStatus())
                .rawPayload(domain.getRawPayload())
                .confirmedBy(confirmedBy)
                .confirmedAt(domain.getConfirmedAt() != null ? domain.getConfirmedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
