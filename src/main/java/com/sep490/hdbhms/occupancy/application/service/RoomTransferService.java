package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.*;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.in.command.*;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.*;
import com.sep490.hdbhms.occupancy.domain.value_objects.*;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SubmitHandoverRequest;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTransferService implements RoomTransferUseCase {
    static final List<LeaseStatus> DESTINATION_BLOCKING_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.CONFIRMED,
            LeaseStatus.SIGNED
    );
    static final List<TransferRequestStatus> ACTIVE_RESERVATION_STATUSES = List.of(
            TransferRequestStatus.WAITING_NEW_CONTRACT,
            TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL,
            TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION,
            TransferRequestStatus.WAITING_SIGNING,
            TransferRequestStatus.WAITING_EXECUTION
    );
    static final List<TransferRequestStatus> OPEN_TRANSFER_STATUSES = List.of(
            TransferRequestStatus.WAITING_APPROVAL,
            TransferRequestStatus.WAITING_NEW_CONTRACT,
            TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL,
            TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION,
            TransferRequestStatus.WAITING_SIGNING,
            TransferRequestStatus.WAITING_EXECUTION
    );
    static final int TRANSFER_RESERVATION_GRACE_DAYS = 1;
    static final int TARGET_HOLDER_APPROVAL_TIMEOUT_DAYS = 7;

    RoomRepository roomRepository;
    TenantRepository tenantRepository;
    PersonProfileRepository personProfileRepository;
    LeaseContractRepository leaseContractRepository;
    RoomTransferRepository roomTransferRepository;
    ChangeRequestRepository changeRequestRepository;
    ContractOccupantRepository contractOccupantRepository;
    RoomTransferRequestRepository roomTransferRequestRepository;
    TransferSettlementRepository transferSettlementRepository;
    InvoiceRepository invoiceRepository;
    InvoiceLineRepository invoiceLineRepository;
    ManageContractHandoverService manageContractHandoverService;
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    @Transactional
    public Long createTransferRequest(CreateTransferRequestCommand command) {
        log.info("Creating room transfer request. userId={}, sourceContractId={}, targetRoomId={}",
                command.requesterId(), command.sourceContractId(), command.targetRoomId());

        Tenant requesterTenant = tenantRepository.findByUserId(command.requesterId())
                .orElseThrow(() -> new AppException(ApiErrorCode.TENANT_NOT_FOUND));
        PersonProfile requesterProfile = personProfileRepository.findByUserId(command.requesterId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        LeaseContract sourceContract = leaseContractRepository.findById(command.sourceContractId())
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));
        Room targetRoom = roomRepository.findById(command.targetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        validateSourceContract(sourceContract, requesterProfile.getId());
        if (roomTransferRequestRepository.existsOpenByOldContractId(sourceContract.getId(), OPEN_TRANSFER_STATUSES)) {
            throw new IllegalStateException("Source contract already has an open room transfer request.");
        }
        if (sourceContract.getRoomId().equals(targetRoom.getId())) {
            throw new IllegalArgumentException("Target room must be different from current room.");
        }
        Optional<LeaseContract> targetActiveLeaseContractResult = leaseContractRepository
                .findFirstActiveContract(
                        targetRoom.getId(),
                        DESTINATION_BLOCKING_CONTRACT_STATUSES
                );
        TargetTransferType targetTransferType = resolveTargetTransferType(
                requesterProfile.getId(),
                targetRoom,
                targetActiveLeaseContractResult
        );

        targetRoom = validateTargetRoomForTransferType(targetRoom, targetTransferType, command.requestedTransferDate());

        List<ContractOccupant> activeOccupants = activeOccupants(sourceContract.getId());
        List<Long> transferringProfileIds = normalizeTransferredProfiles(
                command.transferredTenantProfileIds(),
                activeOccupants
        );
        ensureRequesterIsCurrentOccupant(requesterProfile.getId(), activeOccupants);
        ensureTransferredProfilesBelongToContract(transferringProfileIds, activeOccupants);
        validateDestinationAvailability(targetRoom, transferringProfileIds.size(), command.requestedTransferDate(), null);

        RoomTransferRequest transferRequest = RoomTransferRequest.builder()
                .requestCode(nextTransferCode())
                .requesterId(requesterTenant.getId())
                .oldContractId(sourceContract.getId())
                .oldRoomId(sourceContract.getRoomId())
                .targetRoomId(targetRoom.getId())
                .transferringTenantProfileIds(transferringProfileIds)
                .targetTransferType(targetTransferType)
                .targetContractId(targetTransferType == TargetTransferType.NEW_CONTRACT
                        ? null
                        : targetActiveLeaseContractResult.map(LeaseContract::getId).orElse(null))
                .requestedTransferDate(command.requestedTransferDate())
                .reason(command.reason())
                .status(TransferRequestStatus.WAITING_APPROVAL)
                .build();
        transferRequest = roomTransferRepository.save(transferRequest);

        ChangeRequest changeRequest = ChangeRequest.builder()
                .requestCode(nextChangeRequestCode())
                .requesterId(command.requesterId())
                .requesterRole(RequesterRole.TENANT)
                .requestType(RequestType.ROOM_TRANSFER)
                .targetType(TargetType.CONTRACT)
                .targetId(transferRequest.getId())
                .title(String.format("Chuyển sang phòng %s", targetRoom.getName()))
                .description(command.reason() == null ? "" : command.reason())
                .assignedRole(AssignedRole.MANAGER)
                .status(RequestStatus.PENDING)
                .build();
        changeRequestRepository.save(changeRequest);

        return transferRequest.getId();
    }

    @Override
    @Transactional
    public void approveTransfer(ApproveTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        requireStatus(request, TransferRequestStatus.WAITING_APPROVAL);
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        targetRoom = validateTargetRoomForTransferType(
                targetRoom,
                transferType,
                request.getRequestedTransferDate()
        );
        validateDestinationAvailability(
                targetRoom,
                request.getTransferringTenantProfileIds().size(),
                request.getRequestedTransferDate(),
                request.getId()
        );
        reserveTargetCapacity(request, targetRoom);
        request.setStatus(resolveApprovedStatus(transferType));
        request = roomTransferRepository.save(request);
        if (transferType == TargetTransferType.NEW_CONTRACT && canCreateNewContractDraftImmediately(request)) {
            completeNewContractTransfer(request, command.managerId());
        }
    }

    @Override
    @Transactional
    public void rejectTransferRequest(Long requestId, Long managerId, String resolutionNote) {
        RoomTransferRequest request = getTransfer(requestId);
        requireStatus(request, TransferRequestStatus.WAITING_APPROVAL);
        releaseTargetCapacity(request);
        request.setStatus(TransferRequestStatus.REJECTED);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void nominateHolder(NominateHolderCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        requireHolderNominationOpenStatus(request);
        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        if (!holderNominationRequired(oldContract, request, activeOccupants)) {
            throw new IllegalStateException("Holder nomination is not required for this transfer.");
        }
        if (!remainingProfileIds(request, activeOccupants).contains(command.nominatedHolderProfileId())) {
            throw new IllegalArgumentException("Nominated holder must be one of the remaining occupants.");
        }
        request.setNominatedHolderProfileId(command.nominatedHolderProfileId());
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void acceptHolderNomination(AcceptHolderNominationCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        requireHolderNominationOpenStatus(request);
        PersonProfile nominee = personProfileRepository.findByUserId(command.nominatedHolderId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (!nominee.getId().equals(request.getNominatedHolderProfileId())) {
            throw new IllegalArgumentException("Current user is not the nominated holder.");
        }
    }

    @Override
    @Transactional
    public void completeTransfer(CompleteTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        switch (transferType) {
            case NEW_CONTRACT -> completeNewContractTransfer(request, command.completedById());
            case OWN_CONTRACT, OTHER_CONTRACT -> {
                if (request.getTargetContractId() == null) {
                    throw new IllegalStateException("Target contract is required for existing-contract transfers.");
                }
                if (request.getTargetTransferType() == TargetTransferType.OTHER_CONTRACT
                        && request.getStatus() == TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL) {
                    return;
                }
                if (request.getStatus() != TransferRequestStatus.WAITING_EXECUTION) {
                    throw new IllegalStateException("Existing-contract transfers must be ready for execution.");
                }
            }
            default -> throw new IllegalStateException("Unsupported transfer type.");
        }
    }

    @Override
    @Transactional
    public void confirmTransferContract(Long requestId) {
        RoomTransferRequest request = getTransfer(requestId);
        requireStatus(request, TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
        LeaseContract newContract = getContract(request.getNewContractId());
        newContract.confirmContract();
        leaseContractRepository.save(newContract);
        request.setStatus(TransferRequestStatus.WAITING_SIGNING);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void signTransferContract(Long requestId) {
        RoomTransferRequest request = getTransfer(requestId);
        requireStatus(request, TransferRequestStatus.WAITING_SIGNING);
        LeaseContract newContract = getContract(request.getNewContractId());
        newContract.signContract();
        leaseContractRepository.save(newContract);
        request.setStatus(TransferRequestStatus.WAITING_EXECUTION);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void rejectTransferContract(Long requestId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (request.getStatus() != TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION
                && request.getStatus() != TransferRequestStatus.WAITING_SIGNING) {
            throw new IllegalStateException("Only transfer contracts awaiting confirmation/signing can be rejected.");
        }
        if (request.getNewContractId() != null) {
            LeaseContract newContract = getContract(request.getNewContractId());
            newContract.cancelContract();
            leaseContractRepository.save(newContract);
        }
        releaseTargetCapacity(request);
        request.setStatus(TransferRequestStatus.CANCELLED);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void approveTargetHolderTransfer(Long requestId, Long holderUserId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (request.getTargetTransferType() != TargetTransferType.OTHER_CONTRACT) {
            throw new IllegalStateException("Target holder approval is only required for OTHER_CONTRACT transfers.");
        }
        requireStatus(request, TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL);
        LeaseContract targetContract = getContract(request.getTargetContractId());
        PersonProfile holderProfile = personProfileRepository.findByUserId(holderUserId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (!holderProfile.getId().equals(targetContract.getPrimaryTenantProfileId())) {
            throw new IllegalArgumentException("Only the target contract holder can approve this transfer.");
        }
        request.setTargetHolderApprovedById(holderUserId);
        request.setTargetHolderApprovedAt(LocalDateTime.now());
        createExistingContractTransferAgreement(request, holderUserId);
    }

    @Override
    @Transactional
    public void rejectTargetHolderTransfer(Long requestId, Long holderUserId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (request.getTargetTransferType() != TargetTransferType.OTHER_CONTRACT) {
            throw new IllegalStateException("Target holder rejection is only required for OTHER_CONTRACT transfers.");
        }
        requireStatus(request, TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL);
        LeaseContract targetContract = getContract(request.getTargetContractId());
        PersonProfile holderProfile = personProfileRepository.findByUserId(holderUserId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (!holderProfile.getId().equals(targetContract.getPrimaryTenantProfileId())) {
            throw new IllegalArgumentException("Only the target contract holder can reject this transfer.");
        }
        request.setTargetHolderRejectedAt(LocalDateTime.now());
        releaseTargetCapacity(request);
        request.setStatus(TransferRequestStatus.REJECTED);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void cancelTransferRequest(Long requestId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (request.getStatus() != TransferRequestStatus.WAITING_APPROVAL
                && request.getStatus() != TransferRequestStatus.WAITING_NEW_CONTRACT
                && request.getStatus() != TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL
                && request.getStatus() != TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION
                && request.getStatus() != TransferRequestStatus.WAITING_SIGNING
                && request.getStatus() != TransferRequestStatus.WAITING_EXECUTION) {
            throw new IllegalStateException("Transfer request cannot be cancelled from status " + request.getStatus());
        }
        if (request.getNewContractId() != null) {
            LeaseContract newContract = getContract(request.getNewContractId());
            newContract.cancelContract();
            leaseContractRepository.save(newContract);
        }
        releaseTargetCapacity(request);
        request.setStatus(TransferRequestStatus.CANCELLED);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public int expireTargetHolderApprovals() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(TARGET_HOLDER_APPROVAL_TIMEOUT_DAYS);
        List<RoomTransferRequest> expiredRequests = roomTransferRepository.findByStatusAndUpdatedAtBefore(
                TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL,
                cutoff
        );
        for (RoomTransferRequest request : expiredRequests) {
            releaseTargetCapacity(request);
            request.setStatus(TransferRequestStatus.EXPIRED);
            roomTransferRepository.save(request);
            log.info("Expired room transfer request {} because target holder did not respond in {} days",
                    request.getId(), TARGET_HOLDER_APPROVAL_TIMEOUT_DAYS);
        }
        return expiredRequests.size();
    }

    @Override
    @Transactional
    public void executeTransfer(ExecuteTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        requireStatus(request, TransferRequestStatus.WAITING_EXECUTION);
        if (request.getRequestedTransferDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Transfer date is in the future.");
        }

        LeaseContract oldContract = getContract(request.getOldContractId());
        Room oldRoom = roomRepository.findById(request.getOldRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        validateTargetRoomStatusForExecution(targetRoom, request);
        validateDestinationAvailability(targetRoom, request.getTransferringTenantProfileIds().size(), request.getRequestedTransferDate(), request.getId());

        List<ContractOccupant> oldOccupants = activeOccupants(oldContract.getId());
        List<Long> remainingProfileIds = remainingProfileIds(request, oldOccupants);
        ensureHolderNominationResolved(oldContract, request, oldOccupants);
        submitTransferOutHandover(command, oldContract.getId(), remainingProfileIds.isEmpty());

        switch (transferType) {
            case NEW_CONTRACT -> executeIntoNewContract(request, command, oldContract, oldRoom, targetRoom, oldOccupants, remainingProfileIds);
            case OWN_CONTRACT, OTHER_CONTRACT -> executeIntoExistingContract(request, command, oldContract, oldRoom, targetRoom, oldOccupants, remainingProfileIds);
            default -> throw new IllegalStateException("Unsupported transfer type.");
        }

        request.setStatus(TransferRequestStatus.EXECUTED);
        roomTransferRepository.save(request);
        log.info("Executed room transfer request {}", request.getId());
    }

    private void validateSourceContract(LeaseContract sourceContract, Long requesterProfileId) {
        if (sourceContract.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Source contract must be ACTIVE.");
        }
        if (!sourceContract.getPrimaryTenantProfileId().equals(requesterProfileId)
                && contractOccupantRepository.findFirstByContract_IdAndTenantProfile_IdAndStatus(
                sourceContract.getId(),
                requesterProfileId,
                OccupantStatus.ACTIVE
        ).isEmpty()) {
            throw new IllegalArgumentException("Requester is not an active occupant of the source contract.");
        }
    }

    private TargetTransferType resolveTargetTransferType(
            Long requesterProfileId,
            Room targetRoom,
            Optional<LeaseContract> targetActiveLeaseContractResult
    ) {
        if (targetRoom.getCurrentStatus() == RoomStatus.SOON_VACANT) {
            return TargetTransferType.NEW_CONTRACT;
        }
        if (targetActiveLeaseContractResult.isEmpty()) {
            return TargetTransferType.NEW_CONTRACT;
        }
        LeaseContract targetContract = targetActiveLeaseContractResult.get();
        List<ContractOccupant> targetOccupants = activeOccupants(targetContract.getId());
        boolean requesterAlreadyInTargetContract = targetOccupants.stream()
                .anyMatch(occupant -> occupant.getTenantProfileId().equals(requesterProfileId));
        return requesterAlreadyInTargetContract
                ? TargetTransferType.OWN_CONTRACT
                : TargetTransferType.OTHER_CONTRACT;
    }

    private Room validateTargetRoomForTransferType(
            Room targetRoom,
            TargetTransferType transferType,
            LocalDate requestedTransferDate
    ) {
        return switch (transferType) {
            case NEW_CONTRACT -> validateTargetRoomStatusForNewTransfer(targetRoom, requestedTransferDate);
            case OWN_CONTRACT, OTHER_CONTRACT -> validateTargetRoomStatusForExistingContractTransfer(targetRoom);
        };
    }

    private Room validateTargetRoomStatusForNewTransfer(Room targetRoom, LocalDate requestedTransferDate) {
        targetRoom = releaseExpiredReservationIfPossible(targetRoom);
        if (targetRoom.getCurrentStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalArgumentException("Target room is already occupied.");
        }
        if (targetRoom.getCurrentStatus() == RoomStatus.MAINTENANCE
                || targetRoom.getCurrentStatus() == RoomStatus.EXPIRED
                || targetRoom.getCurrentStatus() == RoomStatus.RESERVED
                || targetRoom.getCurrentStatus() == RoomStatus.RESERVED_FOR_TRANSFER
                || targetRoom.getCurrentStatus() == RoomStatus.ON_HOLD) {
            throw new IllegalArgumentException("Target room is not available for transfer.");
        }
        if (targetRoom.getCurrentStatus() == RoomStatus.SOON_VACANT) {
            LocalDate availableDate = leaseContractRepository
                    .findFirstActiveContract(targetRoom.getId(), DESTINATION_BLOCKING_CONTRACT_STATUSES)
                    .map(entity -> entity.getExpectedVacantDate() == null ? entity.getEndDate() : entity.getExpectedVacantDate())
                    .orElse(LocalDate.now());
            if (requestedTransferDate.isBefore(availableDate)) {
                throw new IllegalArgumentException("Requested transfer date is before target room available date.");
            }
        }

        return targetRoom;
    }

    private Room validateTargetRoomStatusForExistingContractTransfer(Room targetRoom) {
        targetRoom = releaseExpiredReservationIfPossible(targetRoom);
        if (targetRoom.getCurrentStatus() != RoomStatus.OCCUPIED) {
            throw new IllegalArgumentException("Target room must already be occupied for existing-contract transfers.");
        }
        if (targetRoom.getCurrentStatus() == RoomStatus.MAINTENANCE
                || targetRoom.getCurrentStatus() == RoomStatus.EXPIRED
                || targetRoom.getCurrentStatus() == RoomStatus.RESERVED
                || targetRoom.getCurrentStatus() == RoomStatus.RESERVED_FOR_TRANSFER
                || targetRoom.getCurrentStatus() == RoomStatus.ON_HOLD) {
            throw new IllegalArgumentException("Target room is not available for transfer.");
        }
        return targetRoom;
    }

    private void validateTargetRoomStatusForExecution(Room targetRoom, RoomTransferRequest request) {
        targetRoom = releaseExpiredReservationIfPossible(targetRoom);
        if (targetRoom.getCurrentStatus() == RoomStatus.MAINTENANCE
                || targetRoom.getCurrentStatus() == RoomStatus.EXPIRED
                || targetRoom.getCurrentStatus() == RoomStatus.ON_HOLD) {
            throw new IllegalArgumentException("Target room is not available for transfer execution.");
        }
        if (targetRoom.getCurrentStatus() == RoomStatus.RESERVED) {
            throw new IllegalArgumentException("Target room is reserved by another flow.");
        }
        if (targetRoom.getCurrentStatus() == RoomStatus.RESERVED_FOR_TRANSFER && safe(request.getReservedSlots()) <= 0) {
            throw new IllegalArgumentException("Target room is reserved by another transfer.");
        }
    }

    private TransferRequestStatus resolveApprovedStatus(TargetTransferType targetTransferType) {
        if (targetTransferType == null) {
            return TransferRequestStatus.WAITING_NEW_CONTRACT;
        }
        return switch (targetTransferType) {
            case NEW_CONTRACT -> TransferRequestStatus.WAITING_NEW_CONTRACT;
            case OWN_CONTRACT -> TransferRequestStatus.WAITING_EXECUTION;
            case OTHER_CONTRACT -> TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL;
        };
    }

    private boolean canCreateNewContractDraftImmediately(RoomTransferRequest request) {
        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        return !holderNominationRequired(oldContract, request, activeOccupants);
    }

    private void completeNewContractTransfer(RoomTransferRequest request, Long completedById) {
        requireStatus(request, TransferRequestStatus.WAITING_NEW_CONTRACT);
        LeaseContract oldContract = getContract(request.getOldContractId());
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        if (holderNominationRequired(oldContract, request, activeOccupants)
                && request.getNominatedHolderProfileId() == null) {
            throw new IllegalStateException("Holder nomination must be accepted before creating the new contract.");
        }

        Long newHolderProfileId = request.getTransferringTenantProfileIds().contains(oldContract.getPrimaryTenantProfileId())
                ? oldContract.getPrimaryTenantProfileId()
                : request.getTransferringTenantProfileIds().getFirst();

        LeaseContract newContract = LeaseContract.builder()
                .contractCode(oldContract.getContractCode() + "-TR-" + request.getId())
                .roomId(targetRoom.getId())
                .depositAgreementId(oldContract.getDepositAgreementId())
                .primaryTenantProfileId(newHolderProfileId)
                .startDate(request.getRequestedTransferDate())
                .endDate(oldContract.getEndDate())
                .rentStartDate(request.getRequestedTransferDate())
                .monthlyRent(targetRoom.getListedPrice())
                .paymentCycleMonths(oldContract.getPaymentCycleMonths())
                .depositAmount(oldContract.getDepositAmount())
                .previousContractId(oldContract.getId())
                .createdById(completedById)
                .status(LeaseStatus.DRAFT)
                .build();
        newContract = leaseContractRepository.save(newContract);

        request.setNewContractId(newContract.getId());
        request.setStatus(TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
        request = roomTransferRepository.save(request);
        createRentDifferenceSettlement(request, oldContract, newContract, targetRoom, completedById);
    }

    private void createExistingContractTransferAgreement(RoomTransferRequest request, Long createdById) {
        LeaseContract targetContract = getContract(request.getTargetContractId());
        LeaseContract oldContract = getContract(request.getOldContractId());
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Long agreementHolderProfileId = request.getTransferringTenantProfileIds().contains(oldContract.getPrimaryTenantProfileId())
                ? oldContract.getPrimaryTenantProfileId()
                : request.getTransferringTenantProfileIds().getFirst();

        LeaseContract agreement = LeaseContract.builder()
                .contractCode(targetContract.getContractCode() + "-JOIN-TR-" + request.getId())
                .roomId(targetRoom.getId())
                .depositAgreementId(targetContract.getDepositAgreementId())
                .primaryTenantProfileId(agreementHolderProfileId)
                .startDate(request.getRequestedTransferDate())
                .endDate(targetContract.getEndDate())
                .rentStartDate(request.getRequestedTransferDate())
                .monthlyRent(targetContract.getMonthlyRent())
                .paymentCycleMonths(targetContract.getPaymentCycleMonths())
                .depositAmount(targetContract.getDepositAmount())
                .previousContractId(targetContract.getId())
                .createdById(createdById)
                .status(LeaseStatus.DRAFT)
                .build();
        agreement = leaseContractRepository.save(agreement);

        request.setNewContractId(agreement.getId());
        request.setStatus(TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
        roomTransferRepository.save(request);
    }

    private void executeIntoNewContract(
            RoomTransferRequest request,
            ExecuteTransferCommand command,
            LeaseContract oldContract,
            Room oldRoom,
            Room targetRoom,
            List<ContractOccupant> oldOccupants,
            List<Long> remainingProfileIds
    ) {
        LeaseContract newContract = getContract(request.getNewContractId());
        if (newContract.getStatus() != LeaseStatus.SIGNED) {
            throw new IllegalStateException("New contract must be SIGNED before executing transfer.");
        }
        submitTransferInHandover(command.transferInHandover(), newContract.getId(), true);
        moveTransferredOccupantsToNewContract(request, newContract, oldOccupants);

        if (remainingProfileIds.isEmpty()) {
            oldContract.markTransferred();
            oldRoom.releaseRoom();
            leaseContractRepository.save(oldContract);
            roomRepository.save(oldRoom);
        } else if (request.getTransferringTenantProfileIds().contains(oldContract.getPrimaryTenantProfileId())) {
            oldContract.setPrimaryTenantProfileId(request.getNominatedHolderProfileId());
            leaseContractRepository.save(oldContract);
        }

        newContract.activateContract();
        targetRoom.occupyRoom();
        leaseContractRepository.save(newContract);
        roomRepository.save(targetRoom);
        releaseTargetCapacity(request);
    }

    private void executeIntoExistingContract(
            RoomTransferRequest request,
            ExecuteTransferCommand command,
            LeaseContract oldContract,
            Room oldRoom,
            Room targetRoom,
            List<ContractOccupant> oldOccupants,
            List<Long> remainingProfileIds
    ) {
        LeaseContract targetContract = getContract(request.getTargetContractId());
        if (targetContract.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalStateException("Target contract must be ACTIVE for existing-contract transfers.");
        }
        if (request.getTargetTransferType() == TargetTransferType.OTHER_CONTRACT
                && request.getTargetHolderApprovedAt() == null) {
            throw new IllegalStateException("Target holder approval is required before execution.");
        }
        if (request.getNewContractId() != null && getContract(request.getNewContractId()).getStatus() != LeaseStatus.SIGNED) {
            throw new IllegalStateException("Transfer agreement must be SIGNED before executing transfer.");
        }
        submitTransferInHandover(command.transferInHandover(), targetContract.getId(), false);

        moveTransferredOccupantsToExistingContract(request, targetContract, oldOccupants);

        if (remainingProfileIds.isEmpty()) {
            oldContract.markTransferred();
            oldRoom.releaseRoom();
            leaseContractRepository.save(oldContract);
            roomRepository.save(oldRoom);
        } else if (request.getTransferringTenantProfileIds().contains(oldContract.getPrimaryTenantProfileId())) {
            oldContract.setPrimaryTenantProfileId(request.getNominatedHolderProfileId());
            leaseContractRepository.save(oldContract);
        }

        roomRepository.save(targetRoom);
        releaseTargetCapacity(request);
    }

    private void moveTransferredOccupantsToExistingContract(
            RoomTransferRequest request,
            LeaseContract targetContract,
            List<ContractOccupant> oldOccupants
    ) {
        Set<Long> transferringIds = new HashSet<>(request.getTransferringTenantProfileIds());
        Set<Long> targetActiveProfileIds = new HashSet<>(activeOccupants(targetContract.getId()).stream()
                .map(ContractOccupant::getTenantProfileId)
                .toList());
        List<ContractOccupant> newOccupants = new ArrayList<>();
        for (ContractOccupant oldOccupant : oldOccupants) {
            Long profileId = oldOccupant.getTenantProfileId();
            if (!transferringIds.contains(profileId)) {
                continue;
            }
            oldOccupant.moveOut(request.getRequestedTransferDate());
            if (targetActiveProfileIds.contains(profileId)) {
                continue;
            }
            newOccupants.add(ContractOccupant.builder()
                    .contractId(targetContract.getId())
                    .tenantId(oldOccupant.getTenantId())
                    .tenantProfileId(oldOccupant.getTenantProfileId())
                    .occupantRole(profileId.equals(targetContract.getPrimaryTenantProfileId())
                            ? OccupantRole.PRIMARY
                            : OccupantRole.CO_OCCUPANT)
                    .moveInDate(request.getRequestedTransferDate())
                    .status(OccupantStatus.ACTIVE)
                    .build());
        }
        contractOccupantRepository.saveAll(oldOccupants);
        contractOccupantRepository.saveAll(newOccupants);
    }

    private Room releaseExpiredReservationIfPossible(Room targetRoom) {
        if (targetRoom.getCurrentStatus() != RoomStatus.RESERVED_FOR_TRANSFER) {
            return targetRoom;
        }
        long activeReservedSlots = roomTransferRequestRepository.sumActiveReservedSlotsByRoomId(
                targetRoom.getId(),
                ACTIVE_RESERVATION_STATUSES,
                LocalDateTime.now(),
                null
        );
        long currentOccupants = contractOccupantRepository.countActiveOccupantsByRoomId(targetRoom.getId());
        if (activeReservedSlots == 0 && currentOccupants == 0) {
            targetRoom.releaseRoom();
            return roomRepository.save(targetRoom);
        }
        return targetRoom;
    }

    private void validateDestinationAvailability(
            Room targetRoom,
            int incomingCount,
            LocalDate requestedTransferDate,
            Long excludedTransferRequestId
    ) {
        long currentDestinationOccupancy = contractOccupantRepository.countActiveOccupantsByRoomId(targetRoom.getId());
        long reservedSlots = roomTransferRequestRepository.sumActiveReservedSlotsByRoomId(
                targetRoom.getId(),
                ACTIVE_RESERVATION_STATUSES,
                LocalDateTime.now(),
                excludedTransferRequestId
        );
        if (currentDestinationOccupancy + reservedSlots + incomingCount > targetRoom.getMaxOccupants()) {
            throw new IllegalArgumentException("Target room capacity would be exceeded.");
        }
        if (targetRoom.getCurrentStatus() == RoomStatus.SOON_VACANT) {
            LocalDate availableDate = leaseContractRepository
                    .findFirstActiveContract(targetRoom.getId(), DESTINATION_BLOCKING_CONTRACT_STATUSES)
                    .map(entity -> entity.getExpectedVacantDate() == null ? entity.getEndDate() : entity.getExpectedVacantDate())
                    .orElse(LocalDate.now());
            if (requestedTransferDate.isBefore(availableDate)) {
                throw new IllegalArgumentException("Requested transfer date is before target room available date.");
            }
        }
    }

    private void reserveTargetCapacity(RoomTransferRequest request, Room targetRoom) {
        request.setReservedSlots(resolveReservedSlots(request, targetRoom));
        request.setReservationExpiresAt(request.getRequestedTransferDate()
                .plusDays(TRANSFER_RESERVATION_GRACE_DAYS)
                .atTime(23, 59, 59));
        if (targetRoom.getCurrentStatus() == RoomStatus.VACANT) {
            targetRoom.reserveRoomForTransfer();
            roomRepository.save(targetRoom);
        }
    }

    private int resolveReservedSlots(RoomTransferRequest request, Room targetRoom) {
        if (targetRoom.getCurrentStatus() == RoomStatus.VACANT && targetRoom.getMaxOccupants() != null) {
            return targetRoom.getMaxOccupants();
        }
        return request.getTransferringTenantProfileIds().size();
    }

    private void releaseTargetCapacity(RoomTransferRequest request) {
        if (safe(request.getReservedSlots()) <= 0) {
            return;
        }
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        long otherReservedSlots = roomTransferRequestRepository.sumActiveReservedSlotsByRoomId(
                targetRoom.getId(),
                ACTIVE_RESERVATION_STATUSES,
                LocalDateTime.now(),
                request.getId()
        );
        long currentOccupants = contractOccupantRepository.countActiveOccupantsByRoomId(targetRoom.getId());
        if (targetRoom.getCurrentStatus() == RoomStatus.RESERVED_FOR_TRANSFER
                && otherReservedSlots == 0
                && currentOccupants == 0) {
            targetRoom.releaseRoom();
            roomRepository.save(targetRoom);
        }
        request.setReservedSlots(0);
        request.setReservationExpiresAt(null);
    }

    private List<Long> normalizeTransferredProfiles(List<Long> requestedProfileIds, List<ContractOccupant> activeOccupants) {
        if (requestedProfileIds == null || requestedProfileIds.isEmpty()) {
            return activeOccupants.stream()
                    .map(ContractOccupant::getTenantProfileId)
                    .toList();
        }
        return requestedProfileIds.stream().distinct().toList();
    }

    private void ensureRequesterIsCurrentOccupant(Long requesterProfileId, List<ContractOccupant> activeOccupants) {
        boolean isOccupant = activeOccupants.stream()
                .anyMatch(occupant -> occupant.getTenantProfileId().equals(requesterProfileId));
        if (!isOccupant) {
            throw new IllegalArgumentException("Requester is not an active occupant.");
        }
    }

    private void ensureTransferredProfilesBelongToContract(List<Long> transferringProfileIds, List<ContractOccupant> activeOccupants) {
        if (transferringProfileIds.isEmpty()) {
            throw new IllegalArgumentException("At least one occupant must transfer.");
        }
        Set<Long> activeProfileIds = new HashSet<>(activeOccupants.stream()
                .map(ContractOccupant::getTenantProfileId)
                .toList());
        if (!activeProfileIds.containsAll(transferringProfileIds)) {
            throw new IllegalArgumentException("Transferred occupants must belong to the source contract.");
        }
    }

    private boolean holderNominationRequired(
            LeaseContract oldContract,
            RoomTransferRequest request,
            List<ContractOccupant> activeOccupants
    ) {
        return request.getTransferringTenantProfileIds().contains(oldContract.getPrimaryTenantProfileId())
                && !remainingProfileIds(request, activeOccupants).isEmpty();
    }

    private void ensureHolderNominationResolved(
            LeaseContract oldContract,
            RoomTransferRequest request,
            List<ContractOccupant> activeOccupants
    ) {
        if (holderNominationRequired(oldContract, request, activeOccupants)
                && request.getNominatedHolderProfileId() == null) {
            throw new IllegalStateException("Holder nomination is required before executing this transfer.");
        }
    }

    private void requireHolderNominationOpenStatus(RoomTransferRequest request) {
        if (request.getStatus() != TransferRequestStatus.WAITING_NEW_CONTRACT
                && request.getStatus() != TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL
                && request.getStatus() != TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION
                && request.getStatus() != TransferRequestStatus.WAITING_SIGNING
                && request.getStatus() != TransferRequestStatus.WAITING_EXECUTION) {
            throw new IllegalStateException("Holder nomination cannot be changed from status " + request.getStatus());
        }
    }

    private List<Long> remainingProfileIds(RoomTransferRequest request, List<ContractOccupant> activeOccupants) {
        Set<Long> transferringIds = new HashSet<>(request.getTransferringTenantProfileIds());
        return activeOccupants.stream()
                .map(ContractOccupant::getTenantProfileId)
                .filter(profileId -> !transferringIds.contains(profileId))
                .toList();
    }

    private void moveTransferredOccupantsToNewContract(
            RoomTransferRequest request,
            LeaseContract newContract,
            List<ContractOccupant> oldOccupants
    ) {
        Set<Long> transferringIds = new HashSet<>(request.getTransferringTenantProfileIds());
        List<ContractOccupant> newOccupants = new ArrayList<>();
        for (ContractOccupant oldOccupant : oldOccupants) {
            Long profileId = oldOccupant.getTenantProfileId();
            if (!transferringIds.contains(profileId)) {
                continue;
            }
            oldOccupant.moveOut(request.getRequestedTransferDate());

            newOccupants.add(ContractOccupant.builder()
                    .contractId(newContract.getId())
                    .tenantId(oldOccupant.getTenantId())
                    .tenantProfileId(oldOccupant.getTenantProfileId())
                    .occupantRole(profileId.equals(newContract.getPrimaryTenantProfileId())
                            ? OccupantRole.PRIMARY
                            : OccupantRole.CO_OCCUPANT)
                    .moveInDate(request.getRequestedTransferDate())
                    .status(OccupantStatus.ACTIVE)
                    .build());
        }
        contractOccupantRepository.saveAll(oldOccupants);
        contractOccupantRepository.saveAll(newOccupants);
    }

    private List<ContractOccupant> activeOccupants(Long contractId) {
        return contractOccupantRepository.findAllByContractIdAndStatus(contractId, OccupantStatus.ACTIVE);
    }

    private void createRentDifferenceSettlement(
            RoomTransferRequest request,
            LeaseContract oldContract,
            LeaseContract newContract,
            Room targetRoom,
            Long managerId
    ) {
        long oldRent = safe(oldContract.getMonthlyRent());
        long newRent = safe(newContract.getMonthlyRent());
        long difference = newRent - oldRent;

        if (difference > 0) {
            // New room is more expensive — tenant owes the difference
            Invoice invoice = createTransferDifferenceInvoice(request, oldRent, newRent, difference, targetRoom, managerId);
            saveSettlement(request, oldRent, newRent, difference, SettlementType.TENANT_PAY_MORE, invoice.getId(), managerId);
        } else if (difference < 0) {
            // New room is cheaper — tenant gets credit applied to next month's rent
            long credit = Math.abs(difference);
            createNextRentInvoiceWithTransferCredit(request, newContract, targetRoom, credit, managerId);
            saveSettlement(request, oldRent, newRent, difference, SettlementType.CREDIT_NEXT_CONTRACT, null, managerId);
        } else {
            // Same rent — no settlement needed
            saveSettlement(request, oldRent, newRent, 0L, SettlementType.NO_DIFFERENCE, null, managerId);
        }
    }

    private void submitTransferOutHandover(
            ExecuteTransferCommand command,
            Long oldContractId,
            boolean sourceRoomWillBeEmpty
    ) {
        ExecuteTransferCommand.TransferHandoverData payload = command.transferOutHandover();
        if (payload == null) {
            throw new IllegalArgumentException("Transfer-out utility readings are required before executing transfer.");
        }
        if (sourceRoomWillBeEmpty && (payload.assets() == null || payload.assets().isEmpty())) {
            throw new IllegalArgumentException("Room handover assets are required when the source room becomes empty.");
        }
        manageContractHandoverService.submitHandover(
                oldContractId,
                toSubmitHandoverRequest(payload, HandoverType.TRANSFER_OUT)
        );
    }

    private void submitTransferInHandover(
            ExecuteTransferCommand.TransferHandoverData payload,
            Long targetContractId,
            boolean required
    ) {
        if (payload == null) {
            if (required) {
                throw new IllegalArgumentException("Transfer-in utility baseline readings are required for a new target contract.");
            }
            return;
        }
        manageContractHandoverService.submitHandover(
                targetContractId,
                toSubmitHandoverRequest(payload, HandoverType.TRANSFER_IN)
        );
    }

    private SubmitHandoverRequest toSubmitHandoverRequest(
            ExecuteTransferCommand.TransferHandoverData payload,
            HandoverType handoverType
    ) {
        if (payload.electricity() == null || payload.water() == null) {
            throw new IllegalArgumentException("Electricity and water readings are required for transfer handover.");
        }
        SubmitHandoverRequest request = new SubmitHandoverRequest();
        request.setHandoverType(handoverType);
        request.setHandoverDate(payload.handoverDate());
        request.setNote(payload.note());
        request.setElectricity(toMeterInput(payload.electricity()));
        request.setWater(toMeterInput(payload.water()));
        if (payload.assets() != null) {
            request.setAssets(payload.assets().stream()
                    .map(this::toAssetInput)
                    .toList());
        }
        return request;
    }

    private SubmitHandoverRequest.MeterInput toMeterInput(ExecuteTransferCommand.MeterReadingData source) {
        SubmitHandoverRequest.MeterInput target = new SubmitHandoverRequest.MeterInput();
        target.setCurrentValue(source.currentValue());
        target.setPhotoFileId(source.photoFileId());
        target.setReadingDate(source.readingDate());
        return target;
    }

    private SubmitHandoverRequest.AssetInput toAssetInput(ExecuteTransferCommand.AssetData source) {
        SubmitHandoverRequest.AssetInput target = new SubmitHandoverRequest.AssetInput();
        target.setId(source.id());
        target.setAssetName(source.assetName());
        target.setAssetCategory(source.assetCategory());
        target.setQuantity(source.quantity());
        target.setCurrentCondition(source.currentCondition());
        target.setDescription(source.description());
        target.setFileImageId(source.fileImageId());
        return target;
    }

    private Invoice createTransferDifferenceInvoice(
            RoomTransferRequest request,
            long oldRent,
            long newRent,
            long amount,
            Room targetRoom,
            Long managerId
    ) {
        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceCode("INV-TR-" + request.getId() + "-" + snowflakeIdGenerator.next())
                .propertyId(targetRoom.getPropertyId())
                .roomId(targetRoom.getId())
                .leaseContractId(request.getNewContractId())
                .invoiceType(InvoiceType.TRANSFER_DIFFERENCE)
                .billingPeriod(YearMonth.from(request.getRequestedTransferDate()).toString())
                .issueDate(now)
                .dueDate(now.plusDays(7))
                .status(InvoiceStatus.ISSUED)
                .subtotalAmount(amount)
                .discountAmount(0L)
                .totalAmount(amount)
                .paidAmount(0L)
                .remainingAmount(amount)
                .issuedAt(now)
                .createdBy(managerId)
                .build());
        invoiceLineRepository.save(InvoiceLine.builder()
                .invoiceId(invoice.getId())
                .lineType(InvoiceLineType.TRANSFER_DIFFERENCE)
                .description("Room transfer rent difference: new rent " + newRent + " - old rent " + oldRent)
                .quantity(1)
                .unitPrice(amount)
                .sourceType("ROOM_TRANSFER")
                .sourceId(request.getId())
                .build());
        return invoice;
    }

    private Invoice createNextRentInvoiceWithTransferCredit(
            RoomTransferRequest request,
            LeaseContract newContract,
            Room targetRoom,
            long creditAmount,
            Long managerId
    ) {
        YearMonth nextBillingPeriod = YearMonth.from(request.getRequestedTransferDate()).plusMonths(1);
        LocalDateTime issueDate = nextBillingPeriod.atDay(1).atTime(8, 0);
        LocalDateTime dueDate = nextBillingPeriod.atDay(5).atTime(23, 59, 59);
        long rentAmount = safe(newContract.getMonthlyRent());
        long discountAmount = Math.min(creditAmount, rentAmount);
        long totalAmount = rentAmount - discountAmount;
        return invoiceRepository
                .findFirstByLeastContractIdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(
                        newContract.getId(),
                        nextBillingPeriod.toString(),
                        InvoiceType.RENT,
                        InvoiceStatus.DRAFT
                )
                .map(invoice -> applyCreditToExistingRentInvoice(invoice, discountAmount))
                .orElseGet(() -> createCreditedRentInvoice(
                        request,
                        newContract,
                        targetRoom,
                        nextBillingPeriod,
                        issueDate,
                        dueDate,
                        rentAmount,
                        discountAmount,
                        totalAmount,
                        managerId
                ));
    }

    private Invoice createCreditedRentInvoice(
            RoomTransferRequest request,
            LeaseContract newContract,
            Room targetRoom,
            YearMonth nextBillingPeriod,
            LocalDateTime issueDate,
            LocalDateTime dueDate,
            long rentAmount,
            long discountAmount,
            long totalAmount,
            Long managerId
    ) {
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceCode("INV-RENT-TRC-" + request.getId() + "-" + nextBillingPeriod.toString().replace("-", ""))
                .propertyId(targetRoom.getPropertyId())
                .roomId(targetRoom.getId())
                .leaseContractId(newContract.getId())
                .invoiceType(InvoiceType.RENT)
                .billingPeriod(nextBillingPeriod.toString())
                .issueDate(issueDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(rentAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .remainingAmount(totalAmount)
                .createdBy(managerId)
                .build());
        invoiceLineRepository.save(InvoiceLine.builder()
                .invoiceId(invoice.getId())
                .lineType(InvoiceLineType.ROOM_RENT)
                .description("Room rent with transfer credit applied")
                .quantity(1)
                .unitPrice(rentAmount)
                .sourceType("ROOM_TRANSFER_CREDIT")
                .sourceId(request.getId())
                .build());
        return invoice;
    }

    private Invoice applyCreditToExistingRentInvoice(Invoice invoice, long discountAmount) {
        invoice.addDiscountAmount(discountAmount);
        return invoiceRepository.save(invoice);
    }

    private void saveSettlement(
            RoomTransferRequest request,
            long oldRent,
            long newRent,
            long difference,
            SettlementType settlementType,
            Long transferDifferenceInvoiceId,
            Long managerId
    ) {
        transferSettlementRepository.save(TransferSettlement.builder()
                .transferRequestId(request.getId())
                .oldRoomRemainingValue(oldRent)
                .newRoomRequiredValue(newRent)
                .differenceAmount(difference)
                .settlementType(settlementType)
                .transferDifferenceInvoiceId(transferDifferenceInvoiceId)
                .confirmedById(managerId)
                .confirmedAt(LocalDateTime.now())
                .build());
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private long safe(Integer value) {
        return value == null ? 0L : value;
    }

    private RoomTransferRequest getTransfer(Long requestId) {
        return roomTransferRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND));
    }

    private LeaseContract getContract(Long contractId) {
        return leaseContractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));
    }

    private void requireStatus(RoomTransferRequest request, TransferRequestStatus expectedStatus) {
        if (request.getStatus() != expectedStatus) {
            throw new IllegalStateException("Transfer request must be " + expectedStatus + " but was " + request.getStatus());
        }
    }

    private String nextTransferCode() {
        return "TR-" + snowflakeIdGenerator.next();
    }

    private String nextChangeRequestCode() {
        return "CR-" + snowflakeIdGenerator.next();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTransferRequest getTransferRequestById(Long requestId) {
        log.info("Getting transfer request by ID: {}", requestId);
        return roomTransferRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTransferRequest> getPendingTargetHolderApprovals(Long holderUserId) {
        log.info("Getting pending target holder approvals for user: {}", holderUserId);
        return roomTransferRepository.findPendingTargetHolderApprovals(holderUserId);
    }
}
