package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
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
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        user.assignRole(Role.TENANT);
        userRepository.save(user);
        Tenant tenant = Tenant.newTenant(propertyId, userId);
        tenantRepository.save(tenant);
    }
}
