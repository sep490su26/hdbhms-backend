package com.sep490.hdbhms.maintenance.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.maintenance.application.port.out.GetLeaseContractOfTenantPort;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLeaseContractOfTenantAdapter implements GetLeaseContractOfTenantPort {
    TenantRepository tenantRepository;
    LeaseContractRepository leaseContractRepository;
    PersonProfileRepository personProfileRepository;

    @Override
    public List<LeaseContract> execute(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        PersonProfile personProfile = personProfileRepository.findByUserId(tenant.getUserId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        return leaseContractRepository.findAllByTenantPersonProfileId(personProfile.getId());
    }
}
