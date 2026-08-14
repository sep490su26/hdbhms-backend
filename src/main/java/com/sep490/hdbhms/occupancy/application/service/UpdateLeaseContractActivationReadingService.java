package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractActivationReadingUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateLeaseContractActivationReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateLeaseContractActivationReadingService
        implements UpdateLeaseContractActivationReadingUseCase {

    JpaLeaseContractRepository leaseContractRepository;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(
            Long leaseContractId,
            UpdateLeaseContractActivationReadingRequest request
    ) {
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (contract.getStatus() != LeaseStatus.DRAFT
                && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        if (request == null || request.getCurrentValue() == null) {
            contract.setActivationElectricityValue(null);
            contract.setActivationReadingDate(null);
        } else {
            contract.setActivationElectricityValue(request.getCurrentValue());
            contract.setActivationReadingDate(
                    request.getReadingDate() == null ? LocalDate.now() : request.getReadingDate()
            );
        }
        leaseContractRepository.saveAndFlush(contract);
        return getLeaseContractManagementUseCase.findOne(leaseContractId);
    }
}
