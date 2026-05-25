package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentPersistenceMapper {
//    AccountRepository accountRepository;
//    AccountPersistenceMapper accountPersistenceMapper;
//
//    public Payment toDomain(PaymentEntity entity) {
//        return Payment.builder()
//                .id(entity.getId())
//                .provider(entity.getProvider())
//                .accountId(entity.getAccount().getId())
//                .amount(entity.getAmount())
//                .status(entity.getStatus())
//                .lastUpdatedAt(entity.getLastUpdatedAt())
//                .createdAt(entity.getCreatedAt())
//                .build();
//    }
//
//    public PaymentEntity toEntity(Payment domain) {
//        var account = accountPersistenceMapper.toEntity(
//                accountRepository.findById(domain.getAccountId())
//                        .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
//        );
//        return PaymentEntity.builder()
//                .id(domain.getId())
//                .provider(domain.getProvider())
//                .account(account)
//                .amount(domain.getAmount())
//                .status(domain.getStatus())
//                .createdAt(domain.getCreatedAt())
//                .lastUpdatedAt(domain.getLastUpdatedAt())
//                .build();
//    }
}
