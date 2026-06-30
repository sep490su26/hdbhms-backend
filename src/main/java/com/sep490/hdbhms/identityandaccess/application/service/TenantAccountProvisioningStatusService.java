package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaTenantAccountProvisioningRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantAccountProvisioningStatusService {
    JpaTenantAccountProvisioningRepository provisioningRepository;

    @Transactional
    public void markActiveByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        var provisionings = provisioningRepository.findAllByUserId(userId);
        provisionings.forEach(provisioning -> {
            if (provisioning.getStatus() == TenantAccountProvisioningStatus.DISABLED) {
                return;
            }
            provisioning.setStatus(TenantAccountProvisioningStatus.ACTIVE);
            provisioning.setFailedAt(null);
            provisioning.setFailureReason(null);
        });
        provisioningRepository.saveAll(provisionings);
    }
}
