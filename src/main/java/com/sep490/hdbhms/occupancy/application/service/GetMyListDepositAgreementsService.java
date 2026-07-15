package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListDepositAgreementsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListDepositAgreementsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
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
    DepositAgreementRepository depositAgreementRepository;

    @Override
    public Page<DepositAgreement> execute(GetListDepositAgreementsQuery query) {
        if (query.userId() == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        List<Long> ids;
        if (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER) {
            ids = depositAgreementRepository.findAll().stream()
                    .map(DepositAgreement::getId)
                    .toList();
        } else {
            ids = depositAgreementRepository.findAllAccessibleByUserId(user.getId()).stream()
                    .map(DepositAgreement::getId)
                    .toList();
        }
        if (ids.isEmpty()) {
            return Page.empty(query.pageable());
        }

        return depositAgreementRepository.findAll(
                ids,
                query.status(),
                query.statuses(),
                query.search(),
                query.floorId(),
                query.signedFrom(),
                query.signedTo(),
                query.pageable()
        );
    }
}
