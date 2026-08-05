package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetLeaseContractManagementService implements GetLeaseContractManagementUseCase {
    LeaseContractManagementService leaseContractManagementService;

    @Override
    public PageResponse<LeaseContractManagementResponse> findAll(Pageable pageable) {
        return leaseContractManagementService.findAllForManagement(pageable);
    }

    @Override
    public LeaseContractManagementResponse findOne(Long leaseContractId) {
        return leaseContractManagementService.findOne(leaseContractId);
    }
}
