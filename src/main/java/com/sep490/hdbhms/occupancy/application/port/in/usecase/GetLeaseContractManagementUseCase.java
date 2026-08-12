package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface GetLeaseContractManagementUseCase {
    PageResponse<LeaseContractManagementResponse> findAll(Pageable pageable);
    LeaseContractManagementResponse findOne(Long leaseContractId);
}
