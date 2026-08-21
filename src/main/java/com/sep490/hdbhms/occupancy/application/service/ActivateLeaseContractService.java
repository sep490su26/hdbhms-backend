package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.billingandpayment.application.service.BillingManagementService;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.ActivateLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.property.application.service.MeterReadingPeriod;
import com.sep490.hdbhms.property.application.service.MeterUsageCalculator;
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
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaContractHandoverRecordRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ActivateLeaseContractRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
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
    JpaContractHandoverRecordRepository handoverRecordRepository;
    MeterUsageCalculator meterUsageCalculator;
    BillingManagementService billingManagementService;

    @Override
    public LeaseContractManagementResponse execute(
            Long leaseContractId,
            ActivateLeaseContractRequest request
    ) {
        LeaseContractEntity contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            return getLeaseContractManagementUseCase.findOne(leaseContractId);
        }
        boolean transferReSignContract = workflowSupport.isRoomTransferRenewalContract(contract);
        workflowSupport.ensureNotRoomTransferManagedContract(leaseContractId, transferReSignContract);
        if (contract.getStatus() != LeaseStatus.DRAFT
                && contract.getStatus() != LeaseStatus.PENDING_SIGNATURE
                && !(transferReSignContract
                && (contract.getStatus() == LeaseStatus.CONFIRMED
                || contract.getStatus() == LeaseStatus.SIGNED))) {
            throw new AppException(ApiErrorCode.CONTRACT_ACTIVATION_STATUS_INVALID, contract.getStatus());
        }
        if (contract.getSignedFile() == null) {
            throw new AppException(ApiErrorCode.CONTRACT_SIGNED_FILE_REQUIRED);
        }
        if (contract.getPrimaryTenantProfile() == null) {
            throw new AppException(ApiErrorCode.CONTRACT_PRIMARY_TENANT_REQUIRED);
        }
        if (contract.getStartDate() == null || contract.getEndDate() == null || contract.getEndDate().isBefore(contract.getStartDate())) {
            throw new AppException(ApiErrorCode.CONTRACT_DATES_INVALID);
        }
        RoomEntity room = contract.getRoom();
        if (room == null) {
            throw new AppException(ApiErrorCode.CONTRACT_ROOM_REQUIRED);
        }

        boolean transferTargetContract = transferReSignContract
                && isTransferTargetContract(contract.getId());
        if (transferTargetContract && !hasConfirmedSignedMoveInHandover(contract.getId())) {
            throw new AppException(ApiErrorCode.ROOM_TRANSFER_HANDOVER_NOT_CONFIRMED, HandoverType.MOVE_IN);
        }

        boolean renewalActivation = contract.getPreviousContract() != null
                && (room.getCurrentStatus() == RoomStatus.OCCUPIED
                || room.getCurrentStatus() == RoomStatus.EXPIRED
                || (transferReSignContract && room.getCurrentStatus() == RoomStatus.RESERVED_FOR_TRANSFER));
        if (!renewalActivation
                && room.getCurrentStatus() != RoomStatus.RESERVED
                && room.getCurrentStatus() != RoomStatus.VACANT
                && room.getCurrentStatus() != RoomStatus.ON_HOLD) {
            throw new AppException(ApiErrorCode.CONTRACT_ROOM_STATUS_INVALID, room.getCurrentStatus());
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
            if (transferReSignContract) {
                workflowSupport.copyTransferContractOccupants(previousContract, contract);
            } else {
                workflowSupport.copyContractOccupants(previousContract, contract);
            }
            // A transfer can create two child contracts from the same old contract.
            // Activating the first child marks that shared parent RENEWED, but the
            // second child must still be activatable from the same transfer.
            boolean legacyPrematureRenewal = previousContract.getStatus() == LeaseStatus.RENEWED
                    && (transferReSignContract
                    || List.of(LeaseStatus.DRAFT, LeaseStatus.PENDING_SIGNATURE).contains(contract.getStatus()));
            if (!legacyPrematureRenewal
                    && !List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.EXPIRED)
                    .contains(previousContract.getStatus())) {
                throw new AppException(ApiErrorCode.CONTRACT_PREVIOUS_STATUS_INVALID, previousContract.getStatus());
            }
            if (!transferReSignContract) {
                previousContract.setStatus(LeaseStatus.RENEWED);
                leaseContractRepository.saveAndFlush(previousContract);
            }
        }
        if (contract.getPreviousContract() == null || transferReSignContract) {
            saveContractStartReading(room, contract, request);
        }
        log.info("Test");
        contract.setStatus(LeaseStatus.ACTIVE);
        contract.setSignedAt(LocalDateTime.now());
        if (contract.getRentStartDate() == null) {
            contract.setRentStartDate(workflowSupport.resolveRentStartDate(contract.getStartDate()));
        }
        recordInitialRentPayment(contract, request);
        // Flush before the JDBC synchronization below so the just-activated
        // contract is visible when checking both transfer contracts.
        leaseContractRepository.saveAndFlush(contract);

        RoomStatus fromStatus = room.getCurrentStatus();
        room.setCurrentStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        workflowSupport.appendRoomStatusHistory(room.getId(), fromStatus, RoomStatus.OCCUPIED, "Kích hoạt hợp đồng thuê: " + contract.getContractCode());
        workflowSupport.appendContractEvent(contract.getId(), "SIGNED", "Kích hoạt hợp đồng thuê");
        if (previousContract != null) {
            workflowSupport.appendContractEvent(
                    previousContract.getId(),
                    transferReSignContract ? "OCCUPANT_CHANGED" : "RENEWED",
                    (transferReSignContract
                            ? "Đã kích hoạt hợp đồng con trong luồng chuyển phòng; mã hợp đồng mới="
                            : "Đã tái ký hợp đồng; mã hợp đồng mới=") + contract.getId()
            );
        }
        if (transferReSignContract) {
            advanceTransferRequestAfterChildActivation(contract.getId());
        }
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }

    private void recordInitialRentPayment(
            LeaseContractEntity contract,
            ActivateLeaseContractRequest request
    ) {
        // Renewals and transfer re-signings do not collect a new first-cycle rent.
        if (contract.getPreviousContract() != null) {
            return;
        }
        ActivateLeaseContractRequest.InitialRentPayment payment =
                request == null ? null : request.getInitialRentPayment();
        if (payment == null || payment.getAmount() == null) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVATION_INITIAL_RENT_PAYMENT_REQUIRED);
        }

        long monthlyRent = contract.getMonthlyRent() == null ? 0L : contract.getMonthlyRent();
        int paymentCycleMonths = contract.getPaymentCycleMonths() == null
                ? 1
                : Math.max(contract.getPaymentCycleMonths(), 1);
        long expectedAmount;
        try {
            expectedAmount = Math.multiplyExact(monthlyRent, paymentCycleMonths);
        } catch (ArithmeticException exception) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVATION_INITIAL_RENT_PAYMENT_INVALID, monthlyRent);
        }
        if (expectedAmount <= 0 || payment.getAmount() != expectedAmount) {
            throw new AppException(ApiErrorCode.LEASE_ACTIVATION_INITIAL_RENT_PAYMENT_INVALID, expectedAmount);
        }

        billingManagementService.recordActivationRentPayment(
                contract,
                payment.getAmount(),
                payment.getPayerName(),
                payment.getNote(),
                AuthUtils.getCurrentAuthenticationId()
        );
    }

    private void advanceTransferRequestAfterChildActivation(Long leaseContractId) {
        jdbcTemplate.query("""
                        SELECT room_transfer_request_id, new_contract_id, replacement_old_contract_id
                        FROM room_transfer_requests
                        WHERE (new_contract_id = ? OR replacement_old_contract_id = ?)
                          AND status IN ('WAITING_CONTRACT_CONFIRMATION', 'WAITING_SIGNING', 'WAITING_CONTRACT_SIGNING', 'WAITING_TRANSFER_DATE')
                        ORDER BY room_transfer_request_id DESC
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return;
                    }
                    long requestId = rs.getLong("room_transfer_request_id");
                    Long newContractId = getLongOrNull(rs, "new_contract_id");
                    Long replacementContractId = getLongOrNull(rs, "replacement_old_contract_id");
                    List<Long> requiredContractIds = java.util.stream.Stream.of(newContractId, replacementContractId)
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .toList();
                    if (requiredContractIds.isEmpty()) {
                        return;
                    }
                    String placeholders = String.join(",", requiredContractIds.stream().map(id -> "?").toList());
                    Integer activeContractCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM lease_contracts "
                                    + "WHERE lease_contract_id IN (" + placeholders + ") "
                                    + "AND status = 'ACTIVE' "
                                    + "AND signed_file_id IS NOT NULL",
                            Integer.class,
                            requiredContractIds.toArray()
                    );
                    if (activeContractCount == null || activeContractCount != requiredContractIds.size()) {
                        return;
                    }
                    int updatedRows = jdbcTemplate.update("""
                                    UPDATE room_transfer_requests
                                    SET status = 'READY_FOR_HANDOVER', reservation_expires_at = NULL
                                    WHERE room_transfer_request_id = ?
                                      AND status IN ('WAITING_CONTRACT_CONFIRMATION', 'WAITING_SIGNING', 'WAITING_CONTRACT_SIGNING', 'WAITING_TRANSFER_DATE')
                                    """,
                            requestId
                    );
                    if (updatedRows > 0) {
                        log.info("Transfer request {} is ready for handover after all child contracts were activated",
                                requestId);
                    }
                },
                leaseContractId,
                leaseContractId
        );
    }

    private Long getLongOrNull(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private boolean isTransferTargetContract(Long leaseContractId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM room_transfer_requests
                        WHERE new_contract_id = ?
                          AND status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED', 'COMPLETED')
                        """, Integer.class, leaseContractId);
        return count != null && count > 0;
    }

    private boolean hasConfirmedSignedMoveInHandover(Long leaseContractId) {
        return handoverRecordRepository.existsByContract_IdAndHandoverTypeAndStatusAndSignedDocumentIsNotNull(
                leaseContractId,
                HandoverType.MOVE_IN,
                HandoverStatus.CONFIRMED
        );
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
            throw new AppException(ApiErrorCode.CONTRACT_ACTIVATION_READING_REQUIRED);
        }

        LocalDate readingDate = input != null && input.getReadingDate() != null
                ? input.getReadingDate()
                : contract.getActivationReadingDate();
        if (readingDate == null) {
            readingDate = LocalDate.now();
        }
        contract.setActivationElectricityValue(currentValue);
        contract.setActivationReadingDate(readingDate);

        var activeMeter = meterRepository
                .findFirstByRoom_IdAndMeterTypeAndStatus(room.getId(), MeterType.ELECTRICITY, MeterStatus.ACTIVE);
        MeterEntity meter = activeMeter.orElseGet(() -> meterRepository.save(MeterEntity.builder()
                        .room(room)
                        .meterType(MeterType.ELECTRICITY)
                        .status(MeterStatus.ACTIVE)
                        .installedAt(LocalDate.now())
                        .build()));
        BigDecimal previousValue = activeMeter.isPresent()
                ? meterReadingRepository
                .findFirstByMeter_IdAndStatusNotOrderByReadingDateDescCreatedAtDescIdDesc(
                        meter.getId(), ReadingStatus.VOIDED)
                .map(MeterReadingEntity::getCurrentValue)
                .orElse(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        MeterUsageCalculator.Calculation usage = meterUsageCalculator.calculate(
                previousValue,
                currentValue,
                meter.getCounterCapacity(),
                currentValue.compareTo(previousValue) < 0 ? null : 0
        );
        if (!usage.valid()) {
            throw new AppException(ApiErrorCode.CONTRACT_ACTIVATION_READING_INVALID, currentValue, previousValue);
        }
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

        MeterReadingEntity reading = MeterReadingEntity.builder()
                .meter(meter)
                .room(room)
                .readingPeriod(readingPeriod)
                .revisionNo(nextRevision)
                .previousValue(previousValue)
                .currentValue(currentValue)
                .rolloverCount(usage.rolloverCount())
                .counterCapacitySnapshot(usage.rolloverCount() > 0 ? usage.counterCapacity() : BigDecimal.ZERO)
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
