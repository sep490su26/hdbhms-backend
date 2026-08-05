package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadSignedLeaseContractFileUseCase {
    LeaseContractManagementResponse execute(Long leaseContractId, MultipartFile file, boolean replace);
}
