package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLeaseContractDetailsService implements GetLeaseContractDetailsUseCase {
    LeaseContractRepository leaseContractRepository;

    @Override
    public LeaseContract execute(GetLeaseContractDetailsQuery query) {
        return leaseContractRepository.findById(query.leaseContractId())
                .orElseThrow(() -> new IllegalArgumentException("Lease contract not found"));
    }
}
