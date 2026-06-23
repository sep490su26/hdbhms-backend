package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
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
    LeaseContractRepository leaseContractRepository;
    LeaseContractQueryService leaseContractQueryService;

    @Override
    public Page<LeaseContract> execute(GetListLeaseContractsQuery query) {
        if (query.userId() == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        List<Long> ids = leaseContractQueryService.getRentalContexts(query.userId()).stream()
                .map(LeaseContractQueryService.ActiveRoomItem::contractId)
                .toList();
        if (ids.isEmpty()) {
            return Page.empty(query.pageable());
        }

        return leaseContractRepository.findAll(
                ids,
                query.status(),
                query.signedFrom(),
                query.signedTo(),
                query.pageable()
        );
    }
}
