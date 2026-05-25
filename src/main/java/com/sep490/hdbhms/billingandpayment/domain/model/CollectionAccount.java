package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.CollectionAccountProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.CollectionAccountType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
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
