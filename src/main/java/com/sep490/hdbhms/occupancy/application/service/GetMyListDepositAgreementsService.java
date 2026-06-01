package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListDepositAgreementsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListDepositAgreementsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
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
public class GetMyListDepositAgreementsService implements GetMyListDepositAgreementsUseCase {
    UserRepository userRepository;
    TenantRepository tenantRepository;
    DepositAgreementRepository depositAgreementRepository;

    @Override
    public Page<DepositAgreement> execute(GetListDepositAgreementsQuery query) {
        if (query.userId() == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Tenant Not Found"));
        List<Long> ids = depositAgreementRepository.findAllByTenantId(tenant.getId()).stream()
                .map(DepositAgreement::getId)
                .toList();

        return depositAgreementRepository.findAll(
                ids,
                query.status(),
                query.signedFrom(),
                query.signedTo(),
                query.pageable()
        );
    }
}
