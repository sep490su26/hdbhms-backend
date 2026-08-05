package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadSignedLeaseContractForDepositUseCase {
    LeaseContractManagementResponse execute(Long depositAgreementId, MultipartFile file);
}
