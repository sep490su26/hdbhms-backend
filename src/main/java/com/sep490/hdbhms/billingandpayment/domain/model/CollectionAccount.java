package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.CollectionAccountProvider;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.CollectionAccountType;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionAccount {
    Long id;
    Long propertyId;
    CollectionAccountType accountType;
    String bankName;
    String accountNumber;
    String accountHolder;
    CollectionAccountProvider provider;
    AccountStatus status;
    LocalDateTime createdAt;
}
