package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.utils.AuthUtils;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.ActivateLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.application.service.MeterReadingPeriod;
import com.sep490.hdbhms.property.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import com.sep490.hdbhms.property.domain.value_objects.ReadingPurpose;
import com.sep490.hdbhms.property.domain.value_objects.ReadingStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaMeterRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ActivateLeaseContractRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ActivateLeaseContractService implements ActivateLeaseContractUseCase {
    JpaLeaseContractRepository leaseContractRepository;
    JpaRoomRepository roomRepository;
    JpaMeterRepository meterRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaUserRepository userRepository;
    JdbcTemplate jdbcTemplate;
    LeaseContractWorkflowSupport workflowSupport;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(
            Long leaseContractId,
            ActivateLeaseContractRequest request
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return getLeaseContractManagementUseCase.findOne(leaseContractId);
        }
        workflowSupport.ensureNotRoomTransferManagedContract(leaseContractId);
        if (contract.getStatus() != LeaseStatus.DRAFT && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (contract.getSignedFile() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (contract.getPrimaryTenantProfile() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (contract.getStartDate() == null || contract.getEndDate() == null || contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        boolean renewalActivation = contract.getPreviousContract() != null
                && (room.getCurrentStatus() == RoomStatus.OCCUPIED
                || room.getCurrentStatus() == RoomStatus.EXPIRED);
        if (!renewalActivation
                && room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        Long previousContractId = contract.getPreviousContract() != null
                ? contract.getPreviousContract().getId()
                : null;
//        if (workflowSupport.hasOtherActiveContract(room.getId(), contract.getId(), previousContractId)) {
//            throw new AppException(ApiErrorCode.INVALID_REQUEST);
//        }

        workflowSupport.ensureContractOccupants(contract);
        LeaseContractEntity previousContract = contract.getPreviousContract();
//        if (previousContract != null && workflowSupport.isHolderReplacementLiquidation(previousContract.getId())) {
//            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
//        }
        if (previousContract != null) {
            workflowSupport.copyContractOccupants(previousContract, contract);
            boolean legacyPrematureRenewal = previousContract.getStatus() == LeaseStatus.RENEWED
                    && List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE).contains(contract.getStatus());
            if (!legacyPrematureRenewal
                    && !List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                    .contains(previousContract.getStatus())) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
            previousContract.setStatus(LeaseStatus.RENEWED);
            leaseContractRepository.saveAndFlush(previousContract);
        }
        if (contract.getPreviousContract() == null) {
            saveContractStartReading(room, contract, request);
        }
        log.info("Test");
        contract.setStatus(LeaseStatus.ACTIVE);
        contract.setSignedAt(LocalDateTime.now());
        if (contract.getRentStartDate() == null) {
            contract.setRentStartDate(workflowSupport.resolveRentStartDate(contract.getStartDate()));
        }
        leaseContractRepository.save(contract);

        RoomStatus fromStatus = room.getCurrentStatus();
        room.setCurrentStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        workflowSupport.appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.OCCUPIED, "Kích hoạt hợp đồng thuê: " + contract.getContractCode());
        workflowSupport.appendContractEvent(contract.getId(), "SIGNED", "Kích hoạt hợp đồng thuê");
        if (previousContract != null) {
            workflowSupport.appendContractEvent(previousContract.getId(), "RENEWED", "Đã tái ký hợp đồng; mã hợp đồng mới=" + contract.getId());
        }
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private MeterReadingEntity saveContractStartReading(
            RoomEntity room,
            LeaseContractEntity contract,
            ActivateLeaseContractRequest request
    ) {
        ActivateLeaseContractRequest.MeterInput input = request == null ? null : request.getElectricity();
        BigDecimal currentValue = input != null && input.getCurrentValue() != null
                ? input.getCurrentValue()
                : contract.getActivationElectricityValue();
        if (currentValue == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        LocalDate readingDate = input != null && input.getReadingDate() != null
                ? input.getReadingDate()
                : contract.getActivationReadingDate();
        if (readingDate == null) {
            readingDate = LocalDate.now();
        }
        contract.setActivationElectricityValue(currentValue);
        contract.setActivationReadingDate(readingDate);

        MeterEntity meter = meterRepository
                .findFirstByRoom_IdAndMeterTypeAndStatus(room.getId(), MeterType.ELECTRICITY, MeterStatus.ACTIVE)
                .orElseGet(() -> meterRepository.save(MeterEntity.builder()
                        .room(room)
                        .meterType(MeterType.ELECTRICITY)
                        .status(MeterStatus.ACTIVE)
                        .installedAt(LocalDate.now())
                        .build()));
        String readingPeriod = MeterReadingPeriod.from(readingDate);
        int nextRevision = 1;
        var periodReading = meterReadingRepository
                .findFirstByMeter_IdAndReadingPeriodOrderByRevisionNoDesc(meter.getId(), readingPeriod);
        if (periodReading.isPresent()) {
            MeterReadingEntity existing = periodReading.get();
            nextRevision = existing.getRevisionNo() + 1;
            if (existing.getStatus() != ReadingStatus.VOIDED) {
                existing.setStatus(ReadingStatus.VOIDED);
                existing.setVoidReason("Bị thay thế bởi chỉ số điện đầu kỳ của hợp đồng mới");
                meterReadingRepository.saveAndFlush(existing);
            }
        }

        BigDecimal previousValue = meterReadingRepository
                .findFirstByRoom_IdAndMeter_MeterTypeAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        room.getId(), MeterType.ELECTRICITY, ReadingStatus.VOIDED)
                .map(MeterReadingEntity::getCurrentValue)
                .orElse(BigDecimal.ZERO);
        MeterReadingEntity reading = MeterReadingEntity.builder()
                .meter(meter)
                .room(room)
                .readingPeriod(readingPeriod)
                .revisionNo(nextRevision)
                .previousValue(previousValue)
                .currentValue(currentValue)
                .readingDate(readingDate)
                .purpose(ReadingPurpose.CONTRACT_START)
                .status(ReadingStatus.CONFIRMED)
                .createdBy(AuthUtils.getCurrentAuthenticationId() == null
                        ? null
                        : userRepository.getReferenceById(AuthUtils.getCurrentAuthenticationId()))
                .build();
        if (input != null && input.getPhotoFileId() != null) {
            reading.setPhotoFile(fileMetadataRepository.getReferenceById(input.getPhotoFileId()));
        }
        return meterReadingRepository.save(reading);
    }
}
