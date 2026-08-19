package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractActivationReadingUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateLeaseContractActivationReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import com.sep490.hdbhms.property.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.property.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.property.application.service.MeterUsageCalculator;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateLeaseContractActivationReadingService
        implements UpdateLeaseContractActivationReadingUseCase {

    JpaLeaseContractRepository leaseContractRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaMeterRepository meterRepository;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;
    MeterUsageCalculator meterUsageCalculator;

    @Override
    public LeaseContractManagementResponse execute(
            Long leaseContractId,
            UpdateLeaseContractActivationReadingRequest request
    ) {
        LeaseContractEntity contract = leaseContractRepository.findByIdAndDeletedAtIsNull(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        boolean transferReSignContract = workflowSupport.isRoomTransferRenewalContract(contract);
        if (contract.getStatus() != LeaseStatus.DRAFT
                && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE
                && !(transferReSignContract
                && (contract.getStatus() == LeaseStatus.CONFIRMED
                || contract.getStatus() == LeaseStatus.SIGNED))) {
            throw new AppException(ApiErrorCode.CONTRACT_ACTIVATION_STATUS_INVALID, contract.getStatus());
        }

        if (request == null || request.getCurrentValue() == null) {
            contract.setActivationElectricityValue(null);
            contract.setActivationReadingDate(null);
        } else {
            if (contract.getRoom() == null) {
                throw new AppException(ApiErrorCode.CONTRACT_ROOM_REQUIRED);
            }
            var activeMeter = meterRepository.findFirstByRoom_IdAndMeterTypeAndStatus(
                    contract.getRoom().getId(),
                    MeterType.ELECTRICITY,
                    MeterStatus.ACTIVE
            ).orElse(null);
            var latestReading = activeMeter == null
                    ? null
                    : meterReadingRepository.findFirstByMeter_IdAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                            activeMeter.getId(),
                            ReadingStatus.VOIDED
                    ).orElse(null);
            BigDecimal previousValue = latestReading == null ? BigDecimal.ZERO : latestReading.getCurrentValue();
            BigDecimal capacity = latestReading == null || latestReading.getMeter() == null
                    ? BigDecimal.valueOf(100000)
                    : latestReading.getMeter().getCounterCapacity();
            if (!meterUsageCalculator.calculate(
                    previousValue,
                    request.getCurrentValue(),
                    capacity,
                    request.getCurrentValue().compareTo(previousValue) < 0 ? null : 0
            ).valid()) {
                throw new AppException(ApiErrorCode.INVALID_METER_READING_VALUE);
            }
            contract.setActivationElectricityValue(request.getCurrentValue());
            contract.setActivationReadingDate(
                    request.getReadingDate() == null ? LocalDate.now() : request.getReadingDate()
            );
        }
        leaseContractRepository.saveAndFlush(contract);
        return getLeaseContractManagementUseCase.findOne(leaseContractId);
    }
}
