package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.CollectionAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCollectionAccountRepository extends JpaRepository<CollectionAccountEntity, Long> {
}
