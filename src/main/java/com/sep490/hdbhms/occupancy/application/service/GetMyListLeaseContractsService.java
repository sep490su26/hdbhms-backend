package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetMyListLeaseContractsService implements GetMyListLeaseContractsUseCase {
    UserRepository userRepository;
    TenantRepository tenantRepository;
    PersonProfileRepository personProfileRepository;
    LeaseContractRepository leaseContractRepository;

    @Override
    public Page<LeaseContract> execute(GetListLeaseContractsQuery query) {
        if (query.userId() == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Tenant Not Found"));
        PersonProfile tenantPersonProfile = personProfileRepository.findByUserId(tenant.getUserId())
                .orElseThrow(() -> new RuntimeException("Person Profile Not Found"));
        List<Long> ids = leaseContractRepository.findAllByTenantProfileId(tenantPersonProfile.getId()).stream()
                .map(LeaseContract::getId)
                .toList();

        return leaseContractRepository.findAll(
                ids,
                query.status(),
                query.signedFrom(),
                query.signedTo(),
                query.pageable()
        );
    }
}
