package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.occupancy.application.port.out.PromoteToTenantPort;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromoteToTenantAdapter implements PromoteToTenantPort {
    UserRepository userRepository;
    TenantRepository tenantRepository;

    @Override
    public void execute(Long propertyId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.TENANT_NOT_FOUND));
        if (user.getRole() != Role.TENANT) {
            user.assignRole(Role.TENANT);
            userRepository.save(user);
        }
        tenantRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseGet(() -> tenantRepository.save(Tenant.newTenant(propertyId, userId)));
    }
}
