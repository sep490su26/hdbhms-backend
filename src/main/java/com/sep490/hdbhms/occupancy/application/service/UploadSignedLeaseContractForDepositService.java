package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateDraftLeaseContractForDepositUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UploadSignedLeaseContractForDepositUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UploadSignedLeaseContractFileUseCase;
import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositAgreementEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadSignedLeaseContractForDepositService implements UploadSignedLeaseContractForDepositUseCase {
    LeaseContractWorkflowSupport workflowSupport;
    CreateDraftLeaseContractForDepositUseCase createDraftLeaseContractForDepositUseCase;
    UploadSignedLeaseContractFileUseCase uploadSignedLeaseContractFileUseCase;

    @Override
    public LeaseContractManagementResponse execute(Long depositAgreementId, MultipartFile file) {
        DepositAgreementEntity deposit = workflowSupport.getReadyDeposit(depositAgreementId);
        LeaseContractEntity contract = workflowSupport.findLatestContractByDeposit(depositAgreementId);
        if (contract == null) {
            createDraftLeaseContractForDepositUseCase.execute(deposit.getId());
            contract = workflowSupport.findLatestContractByDeposit(depositAgreementId);
        }
        return uploadSignedLeaseContractFileUseCase.execute(contract.getId(), file, false);
    }
}
