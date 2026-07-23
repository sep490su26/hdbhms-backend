package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.service.IssuedInvoiceChargeService;
import com.sep490.hdbhms.billingandpayment.application.service.UtilityBillingRunService;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceReason;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.*;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.application.port.in.command.*;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.event.RoomTransferHolderNominationRequestedEvent;
import com.sep490.hdbhms.occupancy.domain.event.RoomTransferManagerActionRequiredEvent;
import com.sep490.hdbhms.occupancy.domain.event.RoomTransferTargetHolderApprovalRequestedEvent;
import com.sep490.hdbhms.occupancy.domain.model.*;
import com.sep490.hdbhms.occupancy.domain.value_objects.*;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.SubmitHandoverRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.ContractHandoverDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.SubmitHandoverResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TransferOutUtilityEstimateResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            TransferRequestStatus.MANAGER_APPROVED,
            TransferRequestStatus.WAITING_HOLDER_RESPONSE,
            TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL,
            TransferRequestStatus.WAITING_TENANT_CONFIRMATION,
            TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION,
            TransferRequestStatus.WAITING_SIGNING,
            TransferRequestStatus.WAITING_CONTRACT_SIGNING,
            TransferRequestStatus.WAITING_PAYMENT,
            TransferRequestStatus.WAITING_TRANSFER_DATE,
            TransferRequestStatus.WAITING_EXECUTION,
            TransferRequestStatus.READY_FOR_HANDOVER
    );
    static final List<TransferRequestStatus> OPEN_TRANSFER_STATUSES = List.of(
            TransferRequestStatus.REQUESTED,
            TransferRequestStatus.MANAGER_APPROVED,
            TransferRequestStatus.WAITING_HOLDER_RESPONSE,
            TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL,
            TransferRequestStatus.WAITING_TENANT_CONFIRMATION,
            TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION,
            TransferRequestStatus.WAITING_SIGNING,
            TransferRequestStatus.WAITING_CONTRACT_SIGNING,
            TransferRequestStatus.WAITING_PAYMENT,
            TransferRequestStatus.WAITING_TRANSFER_DATE,
            TransferRequestStatus.WAITING_EXECUTION,
            TransferRequestStatus.READY_FOR_HANDOVER,
            TransferRequestStatus.WAITING_NEW_CONTRACT
    );
    static final int TRANSFER_RESERVATION_GRACE_DAYS = 1;
    static final int TARGET_HOLDER_APPROVAL_TIMEOUT_DAYS = 7;
    static final int SOURCE_HOLDER_NOMINATION_TIMEOUT_DAYS = 3;
    static final int TRANSFER_BLOCKED_START_DAY_OF_MONTH = 1;
    static final int TRANSFER_BLOCKED_END_DAY_OF_MONTH = 5;
    static final String ACTION_REVIEW_REQUEST = "REVIEW_REQUEST";
    static final String ACTION_SOURCE_HOLDER_REJECTED = "SOURCE_HOLDER_REJECTED";
    static final String ACTION_SOURCE_HOLDER_NOMINATION_EXPIRED = "SOURCE_HOLDER_NOMINATION_EXPIRED";
    static final String ACTION_UPLOAD_SIGNED_CONTRACTS = "UPLOAD_SIGNED_CONTRACTS";
    static final String ACTION_READY_FOR_HANDOVER = "READY_FOR_HANDOVER";
    static final String SOURCE_ROOM_TRANSFER_COMPENSATION = "ROOM_TRANSFER_COMPENSATION";

    RoomRepository roomRepository;
    TenantRepository tenantRepository;
    PersonProfileRepository personProfileRepository;
    UserRepository userRepository;
    LeaseContractRepository leaseContractRepository;
    RoomTransferRepository roomTransferRepository;
    ChangeRequestRepository changeRequestRepository;
    ContractOccupantRepository contractOccupantRepository;
    RoomTransferRequestRepository roomTransferRequestRepository;
    TransferSettlementRepository transferSettlementRepository;
    DepositTransferRecordRepository depositTransferRecordRepository;
    InvoiceRepository invoiceRepository;
    InvoiceLineRepository invoiceLineRepository;
    IssuedInvoiceChargeService issuedInvoiceChargeService;
    UtilityBillingRunService utilityBillingRunService;
    ManageContractHandoverService manageContractHandoverService;
    SnowflakeIdGenerator snowflakeIdGenerator;
    ApplicationEventPublisher applicationEventPublisher;
    JdbcTemplate jdbcTemplate;
    ObjectMapper objectMapper;

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

        validateRequestedTransferDate(command.requestedTransferDate());
        targetRoom = validateTargetRoomForTransferType(targetRoom, targetTransferType, command.requestedTransferDate());

        List<ContractOccupant> activeOccupants = activeOccupants(sourceContract.getId());
        List<Long> transferringProfileIds = normalizeTransferredProfiles(
                command.transferredTenantProfileIds(),
                requesterProfile.getId()
        );
        ensureRequesterIsCurrentOccupant(requesterProfile.getId(), activeOccupants);
        ensureTransferredProfilesBelongToContract(transferringProfileIds, activeOccupants);
        validateTransferEligibilityWindow(sourceContract, transferringProfileIds, activeOccupants);
        validateDestinationAvailability(targetRoom, transferringProfileIds.size(), command.requestedTransferDate(), null);
        LocalDateTime eligibilityCheckedAt = LocalDateTime.now();
        DebtSnapshotDetails debtSnapshot = readDebtSnapshotDetails(sourceContract);
        Long debtSnapshotId = saveDebtSnapshot(sourceContract, debtSnapshot, eligibilityCheckedAt.toLocalDate());
        String violationSnapshot = buildViolationSnapshot(sourceContract.getId());
        String transferHistorySnapshot = buildTransferHistorySnapshot(requesterTenant.getId());
        String eligibilitySnapshot = buildEligibilitySnapshot(
                sourceContract,
                targetRoom,
                targetTransferType,
                transferringProfileIds,
                activeOccupants,
                debtSnapshot,
                violationSnapshot,
                transferHistorySnapshot,
                true,
                List.of(),
                eligibilityCheckedAt
        );

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
                .status(TransferRequestStatus.REQUESTED)
                .debtSnapshotId(debtSnapshotId)
                .eligibilityCheckedAt(eligibilityCheckedAt)
                .eligibleAtCreation(true)
                .eligibilitySnapshot(eligibilitySnapshot)
                .violationSnapshot(violationSnapshot)
                .transferHistorySnapshot(transferHistorySnapshot)
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
        publishManagerActionRequired(transferRequest, ACTION_REVIEW_REQUEST);

        return transferRequest.getId();
    }

    @Override
    @Transactional
    public void confirmTenantTransfer(ConfirmTenantTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        requireStatus(
                request,
                TransferRequestStatus.MANAGER_APPROVED,
                TransferRequestStatus.WAITING_TENANT_CONFIRMATION,
                TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION
        );
        LeaseContract oldContract = getContract(request.getOldContractId());
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());

        requireTransferringTenant(request, command.tenantId());
        applyHolderNominationFromTenantConfirmation(command, request, oldContract, activeOccupants);

        TransferRentDifference rentDifference = calculateTransferRentDifference(
                oldContract,
                null,
                targetRoom,
                request.getRequestedTransferDate()
        );
        long difference = rentDifference.differenceAmount();

        validateSettlementType(difference, command.settlementType());
        if (isWaitingForImmediateDifferencePayment(request, difference)) {
            if (command.settlementType() != SettlementType.TENANT_PAY_MORE) {
                throw new IllegalStateException("Transfer difference invoice has already been created for immediate payment.");
            }
            request.setStatus(TransferRequestStatus.WAITING_TENANT_CONFIRMATION);
            roomTransferRepository.save(request);
            return;
        }

        request.setPositiveDifferenceSettlementType(resolveSettlementType(difference, command.settlementType()));
        if (difference > 0 && request.getPositiveDifferenceSettlementType() == SettlementType.TENANT_PAY_MORE) {
            request.setStatus(TransferRequestStatus.WAITING_TENANT_CONFIRMATION);
            roomTransferRepository.save(request);
            createRentDifferenceSettlement(
                    request,
                    oldContract,
                    resolveSettlementTargetContract(request),
                    targetRoom,
                    command.tenantId(),
                    request.getPositiveDifferenceSettlementType()
            );
            return;
        }

        request = roomTransferRepository.save(request);
        request = confirmTransferContractForTenant(request, command.tenantId());

        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        if (difference != 0 && transferType != TargetTransferType.NEW_CONTRACT) {
            createRentDifferenceSettlement(
                    request,
                    oldContract,
                    resolveSettlementTargetContract(request),
                    targetRoom,
                    command.tenantId(),
                    request.getPositiveDifferenceSettlementType()
            );
        }
    }

    private void validateSettlementType(long difference, SettlementType settlementType) {
        if (difference > 0 && settlementType == null) {
            throw new IllegalArgumentException("Settlement type for positive difference is required.");
        }
        if (difference > 0
                && settlementType != SettlementType.TENANT_PAY_MORE
                && settlementType != SettlementType.ADD_TO_NEXT_INVOICE) {
            throw new IllegalArgumentException("Positive difference can only be paid now or added to next invoice.");
        }
        if (difference < 0 && settlementType == null) {
            throw new IllegalArgumentException("Settlement type for negative difference is required.");
        }
        if (difference < 0
                && settlementType != SettlementType.CREDIT_NEXT_CONTRACT) {
            throw new IllegalArgumentException("Negative difference can only be credited to next contract.");
        }
        if (difference == 0 && settlementType != null && settlementType != SettlementType.NO_DIFFERENCE) {
            throw new IllegalArgumentException("Settlement type is not applicable for zero difference.");
        }
    }

    private SettlementType resolveSettlementType(long difference, SettlementType settlementType) {
        return difference == 0 ? SettlementType.NO_DIFFERENCE : settlementType;
    }

    private boolean isWaitingForImmediateDifferencePayment(RoomTransferRequest request, long difference) {
        if (difference <= 0 || request.getPositiveDifferenceSettlementType() != SettlementType.TENANT_PAY_MORE) {
            return false;
        }
        return transferSettlementRepository.findLatestByTransferRequestId(request.getId())
                .map(TransferSettlement::getTransferDifferenceInvoiceId)
                .isPresent();
    }

    private LeaseContract resolveSettlementTargetContract(RoomTransferRequest request) {
        Long contractId = request.getNewContractId() != null
                ? request.getNewContractId()
                : request.getTargetContractId();
        if (contractId == null) {
            return null;
        }
        return getContract(contractId);
    }

    private void requirePositiveDifferenceInvoicePaidBeforeContractConfirmation(RoomTransferRequest request) {
        LeaseContract oldContract = getContract(request.getOldContractId());
        LeaseContract settlementContract = resolveSettlementTargetContract(request);
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        long difference = calculateTransferRentDifference(
                oldContract,
                settlementContract,
                targetRoom,
                request.getRequestedTransferDate()
        ).differenceAmount();
        SettlementType settlementType = request.getPositiveDifferenceSettlementType();
        if (difference <= 0 || settlementType != SettlementType.TENANT_PAY_MORE) {
            return;
        }

        TransferSettlement settlement = transferSettlementRepository.findLatestByTransferRequestId(request.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Transfer difference invoice has not been created yet."
                ));

        Long invoiceId = settlement.getTransferDifferenceInvoiceId();
        if (invoiceId == null) {
            throw new IllegalStateException(
                    "Transfer difference invoice must be paid before contract confirmation."
            );
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("Transfer difference invoice not found."));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new IllegalStateException(
                    "Transfer difference invoice must be fully paid before contract confirmation."
            );
        }
    }

    @Override
    @Transactional
    public void approveTransfer(ApproveTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        requireStatus(request, TransferRequestStatus.REQUESTED, TransferRequestStatus.WAITING_MANAGER_APPROVAL);
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

        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        boolean requiresHolderNomination = holderNominationRequired(oldContract, request, activeOccupants);

        request.setPositiveDifferenceSettlementType(command.positiveDifferenceSettlementType());
        request.setApprovedById(command.managerId());
        request.setApprovedAt(LocalDateTime.now());

        if (requiresHolderNomination && request.getNominatedHolderProfileId() == null) {
            request.setStatus(TransferRequestStatus.MANAGER_APPROVED);
            roomTransferRepository.save(request);
            return;
        }

        request.setStatus(resolveApprovedStatus(transferType));
        request = roomTransferRepository.save(request);

        if (transferType == TargetTransferType.OTHER_CONTRACT) {
            publishTargetHolderApprovalRequested(request);
        }
    }

    @Override
    @Transactional
    public void rejectTransferRequest(Long requestId, Long managerId, String resolutionNote) {
        RoomTransferRequest request = getTransfer(requestId);
        requireStatus(request, TransferRequestStatus.REQUESTED, TransferRequestStatus.MANAGER_APPROVED);
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
        PersonProfile requesterProfile = personProfileRepository.findByUserId(command.requesterId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (!oldContract.getPrimaryTenantProfileId().equals(requesterProfile.getId())) {
            throw new IllegalArgumentException("Only current source holder can nominate replacement holder.");
        }

        if (!holderNominationRequired(oldContract, request, activeOccupants)) {
            throw new IllegalStateException("Replacement holder nomination is not required for this transfer.");
        }

        Set<Long> remainingProfileIds = new HashSet<>(remainingProfileIds(request, activeOccupants));
        Long nominatedHolderProfileId = command.nominatedHolderProfileId();

        if (!remainingProfileIds.contains(nominatedHolderProfileId)) {
            throw new IllegalArgumentException("Nominated holder must be one of remaining active occupants in old room.");
        }

        request.setNominatedHolderProfileId(nominatedHolderProfileId);
        request.setStatus(TransferRequestStatus.WAITING_HOLDER_RESPONSE);
        request = roomTransferRepository.save(request);
        publishHolderNominationRequested(request, command.requesterId());
    }

    @Override
    @Transactional
    public void confirmTransferContract(Long requestId, Long tenantUserId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (isContractAlreadyConfirmed(request)) {
            return;
        }
        requireStatus(request, TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
        confirmTransferContractForTenant(request, tenantUserId);
    }

    @Override
    @Transactional
    public void advanceTransferAfterDifferencePayment(Long requestId, Long tenantUserId) {
        RoomTransferRequest request = roomTransferRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND));
        if (isContractAlreadyConfirmed(request)) {
            return;
        }
        requireStatus(
                request,
                TransferRequestStatus.WAITING_PAYMENT,
                TransferRequestStatus.WAITING_TENANT_CONFIRMATION,
                TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION
        );
        confirmTransferContractForTenant(request, tenantUserId);
    }

    private RoomTransferRequest confirmTransferContractForTenant(RoomTransferRequest request, Long tenantUserId) {
        requireTransferringTenant(request, tenantUserId);
        requirePositiveDifferenceInvoicePaidBeforeContractConfirmation(request);
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        if (transferType != TargetTransferType.NEW_CONTRACT) {
            return confirmExistingContractTransfer(request, tenantUserId);
        }
        if (request.getNewContractId() == null) {
            createMissingNewContractDraft(request);
        }
        ensureReplacementOldContractDraft(request, tenantUserId);
        LeaseContract newContract = getContract(request.getNewContractId());
        confirmDraftContract(newContract);
        confirmReplacementOldContractIfPresent(request);
        return moveToTransferSigningWait(request);
    }

    @Override
    @Transactional
    public void signTransferContract(Long requestId, Long tenantUserId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (!currentUserHasAnyRole("ROLE_OWNER", "ROLE_MANAGER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only owner or manager can confirm signed transfer contract."
            );
        }
        if (request.getStatus() == TransferRequestStatus.READY_FOR_HANDOVER) {
            return;
        }
        requireStatus(request, TransferRequestStatus.WAITING_SIGNING, TransferRequestStatus.WAITING_CONTRACT_SIGNING);
        List<LeaseContract> contracts = requiredSigningContracts(request);
        if (contracts.isEmpty()) {
            throw new IllegalStateException("No transfer contract found for signing.");
        }
        for (LeaseContract contract : contracts) {
            signUploadedContract(contract);
        }
        request.setStatus(TransferRequestStatus.READY_FOR_HANDOVER);
        request.setReservationExpiresAt(null);
        roomTransferRepository.save(request);
        publishManagerActionRequired(request, ACTION_READY_FOR_HANDOVER);
    }

    private boolean currentUserHasAnyRole(String... roles) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        Set<String> expectedRoles = Set.of(roles);
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedRoles.contains(authority.getAuthority()));
    }

    @Override
    @Transactional
    public void rejectTransferContract(Long requestId, Long tenantUserId) {
        RoomTransferRequest request = getTransfer(requestId);
        if (request.getStatus() != TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION
                && request.getStatus() != TransferRequestStatus.WAITING_SIGNING
                && request.getStatus() != TransferRequestStatus.WAITING_CONTRACT_SIGNING) {
            throw new IllegalStateException("Only transfer contracts awaiting confirmation/signing can be rejected.");
        }
        requireTransferringTenant(request, tenantUserId);
        cancelGeneratedTransferContracts(request);
        releaseTargetCapacity(request);
        request.setStatus(TransferRequestStatus.CANCELLED);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void acceptHolderNomination(AcceptHolderNominationCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        requireStatus(request, TransferRequestStatus.WAITING_HOLDER_RESPONSE);

        if (request.getNominatedHolderProfileId() == null) {
            throw new IllegalStateException("No nominated holder found for this transfer.");
        }

        PersonProfile tenantProfile = personProfileRepository.findByUserId(command.tenantId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (!request.getNominatedHolderProfileId().equals(tenantProfile.getId())) {
            throw new IllegalArgumentException("Only nominated holder can accept holder nomination.");
        }

        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);

        if (transferType == TargetTransferType.OTHER_CONTRACT) {
            request.setStatus(TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL);
            request = roomTransferRepository.save(request);
            publishTargetHolderApprovalRequested(request);
            return;
        }

        request.setStatus(TransferRequestStatus.WAITING_TENANT_CONFIRMATION);
        roomTransferRepository.save(request);
    }

    @Override
    @Transactional
    public void rejectHolderNomination(Long requestId, Long tenantUserId) {
        RoomTransferRequest request = getTransfer(requestId);
        requireStatus(request, TransferRequestStatus.WAITING_HOLDER_RESPONSE);

        if (request.getNominatedHolderProfileId() == null) {
            throw new IllegalStateException("No nominated holder found for this transfer.");
        }

        PersonProfile tenantProfile = personProfileRepository.findByUserId(tenantUserId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (!request.getNominatedHolderProfileId().equals(tenantProfile.getId())) {
            throw new IllegalArgumentException("Only nominated holder can reject holder nomination.");
        }

        request.setNominatedHolderProfileId(null);
        request.setStatus(TransferRequestStatus.MANAGER_APPROVED);
        roomTransferRepository.save(request);
        publishManagerActionRequired(request, ACTION_SOURCE_HOLDER_REJECTED);
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
        request.setStatus(TransferRequestStatus.WAITING_TENANT_CONFIRMATION);
        roomTransferRepository.save(request);
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
        requireStatus(request, TransferRequestStatus.REQUESTED);
        cancelGeneratedTransferContracts(request);
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
    public int expireSourceHolderNominations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(SOURCE_HOLDER_NOMINATION_TIMEOUT_DAYS);
        List<RoomTransferRequest> expiredRequests = roomTransferRepository.findByStatusAndUpdatedAtBefore(
                TransferRequestStatus.WAITING_HOLDER_RESPONSE,
                cutoff
        );
        for (RoomTransferRequest request : expiredRequests) {
            request.setNominatedHolderProfileId(null);
            request.setStatus(TransferRequestStatus.MANAGER_APPROVED);
            roomTransferRepository.save(request);
            publishManagerActionRequired(request, ACTION_SOURCE_HOLDER_NOMINATION_EXPIRED);
            log.info("Expired source holder nomination for room transfer request {} after {} days",
                    request.getId(), SOURCE_HOLDER_NOMINATION_TIMEOUT_DAYS);
        }
        return expiredRequests.size();
    }

    @Override
    @Transactional
    public RoomTransferRequest refreshTransferEligibilitySnapshot(Long requestId) {
        RoomTransferRequest request = getTransfer(requestId);
        LeaseContract sourceContract = getContract(request.getOldContractId());
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        List<ContractOccupant> activeOccupants = activeOccupants(sourceContract.getId());
        List<Long> transferringProfileIds = request.getTransferringTenantProfileIds() == null
                ? List.of()
                : request.getTransferringTenantProfileIds();
        LocalDateTime eligibilityCheckedAt = LocalDateTime.now();
        DebtSnapshotDetails debtSnapshot = readDebtSnapshotDetails(sourceContract);
        Long debtSnapshotId = saveDebtSnapshot(sourceContract, debtSnapshot, eligibilityCheckedAt.toLocalDate());
        String violationSnapshot = buildViolationSnapshot(sourceContract.getId());
        String transferHistorySnapshot = buildTransferHistorySnapshot(request.getRequesterId());
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        boolean eligible = true;
        List<String> blockingReasons = new ArrayList<>();

        try {
            validateTransferEligibilityWindow(sourceContract, transferringProfileIds, activeOccupants);
            targetRoom = validateTargetRoomForTransferType(targetRoom, transferType, request.getRequestedTransferDate());
            validateDestinationAvailability(
                    targetRoom,
                    transferringProfileIds.size(),
                    request.getRequestedTransferDate(),
                    request.getId()
            );
        } catch (RuntimeException exception) {
            eligible = false;
            blockingReasons.add(exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage());
        }

        request.setDebtSnapshotId(debtSnapshotId);
        request.setEligibilityCheckedAt(eligibilityCheckedAt);
        request.setViolationSnapshot(violationSnapshot);
        request.setTransferHistorySnapshot(transferHistorySnapshot);
        request.setEligibilitySnapshot(buildEligibilitySnapshot(
                sourceContract,
                targetRoom,
                transferType,
                transferringProfileIds,
                activeOccupants,
                debtSnapshot,
                violationSnapshot,
                transferHistorySnapshot,
                eligible,
                blockingReasons,
                eligibilityCheckedAt
        ));
        return syncPaidTransferDifferenceStatus(roomTransferRepository.save(request));
    }

    @Override
    @Transactional
    public void executeTransfer(ExecuteTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        requireStatus(request, TransferRequestStatus.WAITING_TRANSFER_DATE, TransferRequestStatus.READY_FOR_HANDOVER);

        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> oldOccupants = activeOccupants(oldContract.getId());
        List<Long> remainingProfileIds = remainingProfileIds(request, oldOccupants);
        ensureHolderNominationResolved(oldContract, request, oldOccupants);
        boolean sourceRoomWillBeEmpty = remainingProfileIds.isEmpty();
        if (!sourceRoomWillBeEmpty && safe(command.oldRoomCompensationAmount()) > 0) {
            throw new IllegalArgumentException("Chỉ được ghi nhận bồi thường phòng cũ khi phòng cũ sẽ trống.");
        }
        SubmitHandoverResponse transferOutHandover = submitTransferOutHandover(command, oldContract.getId(), sourceRoomWillBeEmpty);
        Long oldRoomFinalInvoiceId = transferSettlementRepository.findLatestByTransferRequestId(request.getId())
                .map(TransferSettlement::getOldRoomFinalInvoiceId)
                .orElse(null);
        if (oldRoomFinalInvoiceId == null) {
            oldRoomFinalInvoiceId = utilityBillingRunService.issueTransferInvoiceFromReadings(
                    oldContract.getId(),
                    transferOutHandover.getElectricityReadingId(),
                    transferOutHandover.getWaterReadingId(),
                    transferOutHandover.getHandoverDate() == null ? null : transferOutHandover.getHandoverDate().toLocalDate(),
                    command.executedById()
            );
        }
        if (oldRoomFinalInvoiceId != null) {
            saveOldRoomFinalInvoiceSettlement(request, oldRoomFinalInvoiceId, command.executedById());
        }
        if (sourceRoomWillBeEmpty) {
            createOldRoomCompensationInvoiceIfNeeded(
                    request,
                    oldContract,
                    roomRepository.findById(request.getOldRoomId())
                            .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND)),
                    command.oldRoomCompensationAmount(),
                    command.oldRoomCompensationNote(),
                    command.executedById()
            );
        }

        request.setStatus(TransferRequestStatus.WAITING_EXECUTION);
        request.setExecutedAt(LocalDateTime.now());
        roomTransferRepository.save(request);
        log.info("Submitted transfer-out handover for room transfer request {}", request.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public TransferOutUtilityEstimateResponse estimateTransferOutUtility(ExecuteTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        requireStatus(request, TransferRequestStatus.WAITING_TRANSFER_DATE, TransferRequestStatus.READY_FOR_HANDOVER);
        ExecuteTransferCommand.TransferHandoverData payload = command.transferOutHandover();
        if (payload == null) {
            throw new IllegalArgumentException("Transfer-out utility readings are required for estimate.");
        }
        validateTransferHandoverDate(payload, "transfer-out");
        Room oldRoom = roomRepository.findById(request.getOldRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        LeaseContract oldContract = getContract(request.getOldContractId());
        LocalDate handoverDate = payload.handoverDate() == null ? LocalDate.now() : payload.handoverDate();

        TransferOutUtilityEstimateResponse.MeterChargeEstimate electricity = estimateMeterCharge(
                oldRoom.getId(),
                oldRoom.getPropertyId(),
                UtilityType.ELECTRICITY,
                payload.electricity()
        );
        TransferOutUtilityEstimateResponse.MeterChargeEstimate water = estimateMeterCharge(
                oldRoom.getId(),
                oldRoom.getPropertyId(),
                UtilityType.WATER,
                payload.water()
        );
        long incidentalAmount = safe(payload.incidentalChargeAmount());
        ServiceFeeCharge serviceFee = buildTransferServiceFeeCharge(
                oldContract.getId(),
                oldRoom.getPropertyId(),
                YearMonth.from(handoverDate).toString(),
                handoverDate,
                electricity.amount()
        );
        long totalAmount = safe(electricity.amount()) + safe(water.amount()) + incidentalAmount + serviceFee.amount();
        return new TransferOutUtilityEstimateResponse(
                electricity,
                water,
                incidentalAmount,
                serviceFee.amount(),
                totalAmount
        );
    }

    @Override
    @Transactional
    public void completeTransfer(CompleteTransferCommand command) {
        RoomTransferRequest request = getTransfer(command.requestId());
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        requireStatus(request, TransferRequestStatus.WAITING_EXECUTION);
        requireTransferOutUtilityInvoicePaid(request);

        LeaseContract oldContract = getContract(request.getOldContractId());
        Room oldRoom = roomRepository.findById(request.getOldRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        validateTargetRoomStatusForExecution(targetRoom, request);
        validateTargetRoomOccupancyForExecution(targetRoom, request);
        validateDestinationAvailability(targetRoom, request.getTransferringTenantProfileIds().size(), LocalDate.now(), request.getId());

        List<ContractOccupant> oldOccupants = activeOccupants(oldContract.getId());
        List<Long> remainingProfileIds = remainingProfileIds(request, oldOccupants);
        ensureHolderNominationResolved(oldContract, request, oldOccupants);

        ExecuteTransferCommand executeCommand = new ExecuteTransferCommand(
                command.requestId(),
                command.completedById(),
                null,
                command.transferInHandover(),
                command.positiveDifferenceSettlementType(),
                null,
                null
        );

        switch (transferType) {
            case NEW_CONTRACT -> executeIntoNewContract(request, executeCommand, oldContract, oldRoom, targetRoom, oldOccupants, remainingProfileIds);
            case OTHER_CONTRACT -> executeIntoExistingContract(request, executeCommand, oldContract, oldRoom, targetRoom, oldOccupants, remainingProfileIds);
            default -> throw new IllegalStateException("Unsupported transfer type.");
        }

        request.setStatus(TransferRequestStatus.EXECUTED);
        request.setCompletedAt(LocalDateTime.now());
        roomTransferRepository.save(request);
        log.info("Completed room transfer request {}", request.getId());
    }

    private void validateRequestedTransferDate(LocalDate requestedTransferDate) {
        LocalDate today = LocalDate.now();
        if (requestedTransferDate == null) {
            throw new IllegalArgumentException("Expected transfer date is required.");
        }
        if (requestedTransferDate.isBefore(today)) {
            throw new IllegalArgumentException("Expected transfer date must be today or later.");
        }
        int dayOfMonth = requestedTransferDate.getDayOfMonth();
        if (dayOfMonth >= TRANSFER_BLOCKED_START_DAY_OF_MONTH && dayOfMonth <= TRANSFER_BLOCKED_END_DAY_OF_MONTH) {
            throw new IllegalArgumentException(
                    "Expected transfer date cannot be between day "
                            + TRANSFER_BLOCKED_START_DAY_OF_MONTH
                            + " and day "
                            + TRANSFER_BLOCKED_END_DAY_OF_MONTH
                            + " because it overlaps the utility closing and invoice issuing window."
            );
        }
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

    private void validateTransferEligibilityWindow(
            LeaseContract sourceContract,
            List<Long> transferringProfileIds,
            List<ContractOccupant> activeOccupants
    ) {
        LocalDate today = LocalDate.now();
        LocalDate currentRoomMoveInDate = activeOccupants.stream()
                .filter(occupant -> transferringProfileIds.contains(occupant.getTenantProfileId()))
                .map(ContractOccupant::getMoveInDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(sourceContract.getStartDate());

        if (currentRoomMoveInDate != null && today.isBefore(currentRoomMoveInDate.plusMonths(12))) {
            throw new IllegalArgumentException("Tenant must stay in the current room for at least 12 months before requesting another room transfer.");
        }

        LocalDate contractStart = sourceContract.getStartDate();
        LocalDate contractEnd = sourceContract.getEndDate();
        if (contractStart == null || contractEnd == null || !contractEnd.isAfter(contractStart)) {
            return;
        }

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(contractStart, contractEnd) + 1;
        long stayedDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(contractStart, today) + 1);
        long requiredDays = Math.ceilDiv(totalDays * 2, 3);
        if (stayedDays < requiredDays) {
            throw new IllegalArgumentException("Tenant must complete at least two-thirds of the current contract term before requesting a room transfer.");
        }
    }

    private DebtSnapshotDetails readDebtSnapshotDetails(LeaseContract sourceContract) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                    COALESCE(SUM(CASE WHEN invoice_type = 'RENT' THEN remaining_amount ELSE 0 END), 0) AS rent_debt_amount,
                    COALESCE(SUM(CASE WHEN invoice_type = 'UTILITY' THEN remaining_amount ELSE 0 END), 0) AS utility_debt_amount,
                    COALESCE(SUM(CASE WHEN invoice_type NOT IN ('RENT', 'UTILITY') THEN remaining_amount ELSE 0 END), 0) AS other_debt_amount,
                    COUNT(DISTINCT CASE WHEN invoice_type = 'RENT' THEN billing_period END) AS rent_debt_months,
                    COUNT(DISTINCT CASE WHEN invoice_type = 'UTILITY' THEN billing_period END) AS utility_debt_months
                FROM invoices
                WHERE lease_contract_id = ?
                  AND status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
                  AND COALESCE(remaining_amount, 0) > 0
                """, sourceContract.getId());
        long rentDebt = safeNumber(row.get("rent_debt_amount"));
        long utilityDebt = safeNumber(row.get("utility_debt_amount"));
        long otherDebt = safeNumber(row.get("other_debt_amount"));
        long totalDebt = rentDebt + utilityDebt + otherDebt;
        Long debtLimit = sourceContract.getMonthlyRent();
        return new DebtSnapshotDetails(
                rentDebt,
                utilityDebt,
                otherDebt,
                safeInteger(row.get("rent_debt_months")),
                safeInteger(row.get("utility_debt_months")),
                totalDebt,
                debtLimit,
                debtLimit != null && totalDebt > debtLimit
        );
    }

    private Long saveDebtSnapshot(LeaseContract sourceContract, DebtSnapshotDetails snapshot, LocalDate snapshotDate) {
        Optional<Long> existingSnapshotId = jdbcTemplate.query("""
                        SELECT debt_snapshot_id
                        FROM debt_snapshots
                        WHERE room_id = ?
                          AND snapshot_date = ?
                        ORDER BY debt_snapshot_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("debt_snapshot_id"),
                sourceContract.getRoomId(),
                snapshotDate
        ).stream().findFirst();
        if (existingSnapshotId.isPresent()) {
            return existingSnapshotId.get();
        }

        jdbcTemplate.update("""
                INSERT INTO debt_snapshots (
                    room_id,
                    contract_id,
                    snapshot_date,
                    rent_debt_amount,
                    utility_debt_amount,
                    other_debt_amount,
                    rent_debt_months,
                    utility_debt_months,
                    mixed_debt_amount,
                    debt_limit_amount,
                    is_over_limit
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sourceContract.getRoomId(),
                sourceContract.getId(),
                snapshotDate,
                snapshot.rentDebtAmount(),
                snapshot.utilityDebtAmount(),
                snapshot.otherDebtAmount(),
                snapshot.rentDebtMonths(),
                snapshot.utilityDebtMonths(),
                snapshot.totalDebtAmount(),
                snapshot.debtLimitAmount(),
                snapshot.overLimit()
        );
        return jdbcTemplate.query("""
                        SELECT debt_snapshot_id
                        FROM debt_snapshots
                        WHERE room_id = ?
                          AND snapshot_date = ?
                        ORDER BY debt_snapshot_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("debt_snapshot_id"),
                sourceContract.getRoomId(),
                snapshotDate
        ).stream().findFirst().orElse(null);
    }

    private String buildViolationSnapshot(Long contractId) {
        Integer totalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rule_violations
                WHERE contract_id = ?
                  AND status IN ('RECORDED', 'INVOICED')
                """, Integer.class, contractId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT rule_violation_id, violation_date, description, fine_amount, status
                FROM rule_violations
                WHERE contract_id = ?
                  AND status IN ('RECORDED', 'INVOICED')
                ORDER BY violation_date DESC, rule_violation_id DESC
                LIMIT 20
                """, contractId);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("totalCount", totalCount == null ? 0 : totalCount);
        snapshot.put("items", rows);
        return writeSnapshot(snapshot);
    }

    private String buildTransferHistorySnapshot(Long requesterId) {
        Integer totalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM room_transfer_requests
                WHERE requester_id = ?
                  AND created_at >= MAKEDATE(YEAR(CURRENT_DATE), 1)
                  AND status IN ('EXECUTED', 'COMPLETED')
                """, Integer.class, requesterId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT room_transfer_request_id, request_code, old_room_id, target_room_id, status, created_at, executed_at, completed_at
                FROM room_transfer_requests
                WHERE requester_id = ?
                  AND created_at >= MAKEDATE(YEAR(CURRENT_DATE), 1)
                  AND status IN ('EXECUTED', 'COMPLETED')
                ORDER BY created_at DESC, room_transfer_request_id DESC
                LIMIT 20
                """, requesterId);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("countThisYear", totalCount == null ? 0 : totalCount);
        snapshot.put("items", rows);
        return writeSnapshot(snapshot);
    }

    private String buildEligibilitySnapshot(
            LeaseContract sourceContract,
            Room targetRoom,
            TargetTransferType targetTransferType,
            List<Long> transferringProfileIds,
            List<ContractOccupant> activeOccupants,
            DebtSnapshotDetails debtSnapshot,
            String violationSnapshot,
            String transferHistorySnapshot,
            boolean eligible,
            List<String> blockingReasons,
            LocalDateTime checkedAt
    ) {
        LocalDate today = LocalDate.now();
        LocalDate currentRoomMoveInDate = activeOccupants.stream()
                .filter(occupant -> transferringProfileIds.contains(occupant.getTenantProfileId()))
                .map(ContractOccupant::getMoveInDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(sourceContract.getStartDate());
        long totalContractDays = 0L;
        long stayedDays = 0L;
        if (sourceContract.getStartDate() != null && sourceContract.getEndDate() != null) {
            totalContractDays = java.time.temporal.ChronoUnit.DAYS.between(
                    sourceContract.getStartDate(),
                    sourceContract.getEndDate()
            ) + 1;
            stayedDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(sourceContract.getStartDate(), today) + 1);
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("eligible", eligible);
        snapshot.put("checkedAt", checkedAt);
        snapshot.put("blockingReasons", blockingReasons == null ? List.of() : blockingReasons);
        snapshot.put("sourceContractId", sourceContract.getId());
        snapshot.put("sourceRoomId", sourceContract.getRoomId());
        snapshot.put("targetRoomId", targetRoom.getId());
        snapshot.put("targetRoomStatus", targetRoom.getCurrentStatus());
        snapshot.put("targetTransferType", targetTransferType);
        snapshot.put("transferringTenantProfileIds", transferringProfileIds);
        snapshot.put("currentRoomMoveInDate", currentRoomMoveInDate);
        snapshot.put("contractStartDate", sourceContract.getStartDate());
        snapshot.put("contractEndDate", sourceContract.getEndDate());
        snapshot.put("totalContractDays", totalContractDays);
        snapshot.put("stayedDays", stayedDays);
        snapshot.put("debt", debtSnapshot);
        snapshot.put("violationSnapshot", violationSnapshot);
        snapshot.put("transferHistorySnapshot", transferHistorySnapshot);
        return writeSnapshot(snapshot);
    }

    private String writeSnapshot(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize room transfer snapshot.", exception);
        }
    }

    private TargetTransferType resolveTargetTransferType(
            Long requesterProfileId,
            Room targetRoom,
            Optional<LeaseContract> targetActiveLeaseContractResult
    ) {
        if (targetRoom.getCurrentStatus() == RoomStatus.OCCUPIED) {
            throw new IllegalArgumentException("Target room is already occupied.");
        }
        if (targetActiveLeaseContractResult.isPresent() && targetRoom.getCurrentStatus() != RoomStatus.SOON_VACANT) {
            throw new IllegalArgumentException("Target room has an active contract and is not available for transfer.");
        }
        if (targetActiveLeaseContractResult
                .map(LeaseContract::getPrimaryTenantProfileId)
                .filter(requesterProfileId::equals)
                .isPresent()) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        return TargetTransferType.NEW_CONTRACT;
    }

    private Room validateTargetRoomForTransferType(
            Room targetRoom,
            TargetTransferType transferType,
            LocalDate requestedTransferDate
    ) {
        return switch (transferType) {
            case NEW_CONTRACT -> validateTargetRoomStatusForNewTransfer(targetRoom, requestedTransferDate);
            case OTHER_CONTRACT -> validateTargetRoomStatusForExistingContractTransfer(targetRoom);
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
        if (targetRoom.getCurrentStatus() == RoomStatus.SOON_VACANT
                && contractOccupantRepository.countActiveOccupantsByRoomId(targetRoom.getId()) > 0) {
            LocalDate availableDate = leaseContractRepository
                    .findFirstActiveContract(targetRoom.getId(), DESTINATION_BLOCKING_CONTRACT_STATUSES)
                    .map(entity -> entity.getExpectedVacantDate() == null ? entity.getEndDate() : entity.getExpectedVacantDate())
                    .orElse(LocalDate.now());
            if (requestedTransferDate.isBefore(availableDate)) {
                throw new IllegalArgumentException("Expected transfer date is before target room available date.");
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

    private void validateTargetRoomOccupancyForExecution(Room targetRoom, RoomTransferRequest request) {
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        if (transferType != TargetTransferType.NEW_CONTRACT) {
            return;
        }
        long activeOccupants = contractOccupantRepository.countActiveOccupantsByRoomId(targetRoom.getId());
        if (activeOccupants > 0) {
            throw new IllegalArgumentException("Target room must be vacant before starting this transfer session.");
        }
    }

    private TransferRequestStatus resolveApprovedStatus(TargetTransferType targetTransferType) {
        if (targetTransferType == null) {
            return TransferRequestStatus.WAITING_NEW_CONTRACT;
        }
        return switch (targetTransferType) {
            case NEW_CONTRACT -> TransferRequestStatus.WAITING_TENANT_CONFIRMATION;
            case OTHER_CONTRACT -> TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL;
        };
    }

    private void publishTargetHolderApprovalRequested(RoomTransferRequest request) {
        LeaseContract oldContract = getContract(request.getOldContractId());
        LeaseContract targetContract = getContract(request.getTargetContractId());
        PersonProfile requesterProfile = personProfileRepository.findById(oldContract.getPrimaryTenantProfileId())
                .orElse(null);
        PersonProfile targetHolderProfile = personProfileRepository.findById(targetContract.getPrimaryTenantProfileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (targetHolderProfile.getUserId() == null) {
            log.warn("Cannot notify target holder for room transfer {} because profile {} has no linked user",
                    request.getId(), targetHolderProfile.getId());
            return;
        }
        Room oldRoom = roomRepository.findById(request.getOldRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        applicationEventPublisher.publishEvent(new RoomTransferTargetHolderApprovalRequestedEvent(
                request.getId(),
                request.getRequestCode(),
                targetHolderProfile.getUserId(),
                requesterProfile != null ? requesterProfile.getUserId() : null,
                oldRoom.getId(),
                targetRoom.getId(),
                oldRoom.getName(),
                targetRoom.getName(),
                targetContract.getId(),
                request.getRequestedTransferDate()
        ));
    }

    private void publishHolderNominationRequested(RoomTransferRequest request, Long nominatorUserId) {
        if (request.getNominatedHolderProfileId() == null) {
            return;
        }
        PersonProfile nominatedHolderProfile = personProfileRepository.findById(request.getNominatedHolderProfileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (nominatedHolderProfile.getUserId() == null) {
            log.warn("Cannot notify nominated holder for room transfer {} because profile {} has no linked user",
                    request.getId(), nominatedHolderProfile.getId());
            return;
        }
        Room oldRoom = roomRepository.findById(request.getOldRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));

        applicationEventPublisher.publishEvent(new RoomTransferHolderNominationRequestedEvent(
                request.getId(),
                request.getRequestCode(),
                nominatedHolderProfile.getUserId(),
                nominatorUserId,
                nominatedHolderProfile.getId(),
                oldRoom.getId(),
                targetRoom.getId(),
                oldRoom.getName(),
                targetRoom.getName(),
                request.getRequestedTransferDate()
        ));
    }

    private void publishManagerActionRequired(RoomTransferRequest request, String actionType) {
        Room oldRoom = roomRepository.findById(request.getOldRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        List<Long> managerUserIds = managerRecipientIds(oldRoom, targetRoom);
        if (managerUserIds.isEmpty()) {
            log.warn("Cannot notify manager for room transfer {} because no active manager or owner was found",
                    request.getId());
            return;
        }

        applicationEventPublisher.publishEvent(new RoomTransferManagerActionRequiredEvent(
                request.getId(),
                request.getRequestCode(),
                managerUserIds,
                actionType,
                managerActionLabel(actionType),
                oldRoom.getId(),
                targetRoom.getId(),
                oldRoom.getName(),
                targetRoom.getName(),
                request.getRequestedTransferDate()
        ));
    }

    private List<Long> managerRecipientIds(Room oldRoom, Room targetRoom) {
        LinkedHashSet<Long> propertyIds = new LinkedHashSet<>();
        if (oldRoom != null && oldRoom.getPropertyId() != null) {
            propertyIds.add(oldRoom.getPropertyId());
        }
        if (targetRoom != null && targetRoom.getPropertyId() != null) {
            propertyIds.add(targetRoom.getPropertyId());
        }

        LinkedHashSet<Long> recipientIds = new LinkedHashSet<>();
        for (Long propertyId : propertyIds) {
            recipientIds.addAll(jdbcTemplate.queryForList("""
                            SELECT psa.staff_user_id
                            FROM property_staff_assignments psa
                            JOIN users u ON u.user_id = psa.staff_user_id
                            WHERE psa.property_id = ?
                              AND psa.assignment_status = 'ACTIVE'
                              AND psa.assigned_role = 'MANAGER'
                              AND u.status = 'ACTIVE'
                              AND u.deleted_at IS NULL
                            ORDER BY psa.is_primary DESC, psa.property_staff_assignment_id ASC
                            """,
                    Long.class,
                    propertyId
            ));
        }
        if (!recipientIds.isEmpty()) {
            return new ArrayList<>(recipientIds);
        }
        return userRepository.findIdsByRolesAndStatus(
                List.of(Role.OWNER),
                AccountStatus.ACTIVE
        );
    }

    private String managerActionLabel(String actionType) {
        return switch (actionType) {
            case ACTION_REVIEW_REQUEST -> "Duyệt yêu cầu chuyển phòng mới";
            case ACTION_SOURCE_HOLDER_REJECTED -> "Holder mới đã từ chối đề cử, cần chọn lại người đại diện phòng cũ";
            case ACTION_SOURCE_HOLDER_NOMINATION_EXPIRED -> "Đề cử holder mới đã hết hạn, cần chọn lại người đại diện phòng cũ";
            case ACTION_UPLOAD_SIGNED_CONTRACTS -> "Tải bản hợp đồng đã ký trực tiếp";
            case ACTION_READY_FOR_HANDOVER -> "Bắt đầu phiên chuyển phòng";
            default -> "Xử lý yêu cầu chuyển phòng";
        };
    }

    private boolean canCreateNewContractDraftImmediately(RoomTransferRequest request) {
        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        return !holderNominationRequired(oldContract, request, activeOccupants);
    }

    private boolean isContractAlreadyConfirmed(RoomTransferRequest request) {
        return request.getStatus() == TransferRequestStatus.WAITING_SIGNING
                || request.getStatus() == TransferRequestStatus.WAITING_CONTRACT_SIGNING
                || request.getStatus() == TransferRequestStatus.WAITING_TRANSFER_DATE
                || request.getStatus() == TransferRequestStatus.READY_FOR_HANDOVER;
    }

    private RoomTransferRequest confirmExistingContractTransfer(RoomTransferRequest request, Long tenantUserId) {
        if (request.getTargetContractId() == null) {
            throw new IllegalStateException("No target contract found for existing-contract transfer confirmation.");
        }
        LeaseContract targetContract = getContract(request.getTargetContractId());
        if (targetContract.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalStateException("Target contract must be ACTIVE for existing-contract transfer confirmation.");
        }

        Room targetRoom = roomRepository.findById(request.getTargetRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        validateTargetRoomStatusForExecution(targetRoom, request);

        if (request.getNewContractId() == null) {
            createExistingContractTransferAgreementDraft(request, targetContract, tenantUserId);
        }
        ensureReplacementOldContractDraft(request, tenantUserId);
        confirmDraftContract(getContract(request.getNewContractId()));
        confirmReplacementOldContractIfPresent(request);
        return moveToTransferSigningWait(request);
    }

    private RoomTransferRequest moveToTransferSigningWait(RoomTransferRequest request) {
        request.setStatus(TransferRequestStatus.WAITING_SIGNING);
        request = roomTransferRepository.save(request);
        publishManagerActionRequired(request, ACTION_UPLOAD_SIGNED_CONTRACTS);
        return request;
    }

    private void confirmDraftContract(LeaseContract contract) {
        if (contract.getStatus() == LeaseStatus.DRAFT) {
            contract.confirmContract();
            leaseContractRepository.save(contract);
            return;
        }
        if (contract.getStatus() != LeaseStatus.CONFIRMED) {
            throw new IllegalStateException("Only DRAFT or CONFIRMED transfer contracts can be confirmed.");
        }
    }

    private void confirmReplacementOldContractIfPresent(RoomTransferRequest request) {
        if (request.getReplacementOldContractId() == null) {
            return;
        }
        confirmDraftContract(getContract(request.getReplacementOldContractId()));
    }

    private List<LeaseContract> requiredSigningContracts(RoomTransferRequest request) {
        List<LeaseContract> contracts = new ArrayList<>();
        if (request.getNewContractId() != null) {
            contracts.add(getContract(request.getNewContractId()));
        }
        if (request.getReplacementOldContractId() != null) {
            contracts.add(getContract(request.getReplacementOldContractId()));
        }
        return contracts;
    }

    private void signUploadedContract(LeaseContract contract) {
        if (contract.getSignedFileId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng upload file hợp đồng đã ký trước khi xác nhận đã ký."
            );
        }
        if (contract.getStatus() == LeaseStatus.CONFIRMED) {
            contract.signContract();
            leaseContractRepository.save(contract);
            return;
        }
        if (contract.getStatus() != LeaseStatus.SIGNED) {
            throw new IllegalStateException("Only CONFIRMED or SIGNED transfer contracts can be signed.");
        }
    }

    private void cancelGeneratedTransferContracts(RoomTransferRequest request) {
        cancelDepositTransferRecord(request);
        if (request.getNewContractId() != null) {
            LeaseContract newContract = getContract(request.getNewContractId());
            newContract.cancelContract();
            leaseContractRepository.save(newContract);
        }
        if (request.getReplacementOldContractId() != null) {
            LeaseContract replacementContract = getContract(request.getReplacementOldContractId());
            replacementContract.cancelContract();
            leaseContractRepository.save(replacementContract);
        }
    }

    private void createMissingNewContractDraft(RoomTransferRequest request) {
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        if (transferType != TargetTransferType.NEW_CONTRACT) {
            throw new IllegalStateException("No new contract found for confirmation.");
        }
        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        ensureHolderNominationResolved(oldContract, request, activeOccupants);
        createNewContractDraft(request, null, TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
    }

    private void completeNewContractTransfer(RoomTransferRequest request, Long completedById) {
        requireStatus(request, TransferRequestStatus.WAITING_NEW_CONTRACT);
        createNewContractDraft(request, completedById, TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION);
    }

    private void createNewContractDraft(
            RoomTransferRequest request,
            Long completedById,
            TransferRequestStatus nextStatus
    ) {
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
                .depositAgreementId(null)
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
        ensureDepositTransferRecordDraft(request, oldContract, newContract, targetRoom);
        ensureReplacementOldContractDraft(request, completedById);
        request.setStatus(nextStatus);
        roomTransferRepository.save(request);
    }

    private void createExistingContractTransferAgreementDraft(
            RoomTransferRequest request,
            LeaseContract targetContract,
            Long createdById
    ) {
        LeaseContract agreement = LeaseContract.builder()
                .contractCode(targetContract.getContractCode() + "-JOIN-" + request.getId())
                .roomId(targetContract.getRoomId())
                .depositAgreementId(null)
                .primaryTenantProfileId(targetContract.getPrimaryTenantProfileId())
                .startDate(request.getRequestedTransferDate())
                .endDate(targetContract.getEndDate())
                .rentStartDate(targetContract.getRentStartDate())
                .monthlyRent(targetContract.getMonthlyRent())
                .paymentCycleMonths(targetContract.getPaymentCycleMonths())
                .depositAmount(targetContract.getDepositAmount())
                .previousContractId(targetContract.getId())
                .createdById(createdById)
                .status(LeaseStatus.DRAFT)
                .build();
        agreement = leaseContractRepository.save(agreement);
        request.setNewContractId(agreement.getId());
        roomTransferRepository.save(request);
    }

    private void ensureReplacementOldContractDraft(RoomTransferRequest request, Long createdById) {
        if (request.getReplacementOldContractId() != null) {
            return;
        }
        LeaseContract oldContract = getContract(request.getOldContractId());
        List<ContractOccupant> activeOccupants = activeOccupants(oldContract.getId());
        List<Long> remainingProfileIds = remainingProfileIds(request, activeOccupants);
        if (remainingProfileIds.isEmpty()) {
            return;
        }
        Long replacementHolderProfileId = holderNominationRequired(oldContract, request, activeOccupants)
                ? request.getNominatedHolderProfileId()
                : oldContract.getPrimaryTenantProfileId();
        if (replacementHolderProfileId == null) {
            throw new IllegalStateException("Holder nomination must be accepted before creating replacement old-room contract.");
        }
        if (!remainingProfileIds.contains(replacementHolderProfileId)) {
            throw new IllegalStateException("Replacement holder must be one of the remaining occupants.");
        }

        LeaseContract replacementContract = LeaseContract.builder()
                .contractCode(oldContract.getContractCode() + "-RE-" + request.getId())
                .roomId(oldContract.getRoomId())
                .depositAgreementId(null)
                .primaryTenantProfileId(replacementHolderProfileId)
                .startDate(request.getRequestedTransferDate())
                .endDate(oldContract.getEndDate())
                .rentStartDate(request.getRequestedTransferDate())
                .monthlyRent(oldContract.getMonthlyRent())
                .paymentCycleMonths(oldContract.getPaymentCycleMonths())
                .depositAmount(oldContract.getDepositAmount())
                .previousContractId(oldContract.getId())
                .createdById(createdById)
                .status(LeaseStatus.DRAFT)
                .build();
        replacementContract = leaseContractRepository.save(replacementContract);
        request.setReplacementOldContractId(replacementContract.getId());
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
        LocalDate executionDate = submitTransferInHandover(command.transferInHandover(), newContract.getId(), true);
        moveTransferredOccupantsToNewContract(request, newContract, oldOccupants, executionDate);

        if (remainingProfileIds.isEmpty()) {
            oldContract.markTransferred();
            oldRoom.releaseRoom();
            closeSupersededRenewals(oldContract, executionDate);
            leaseContractRepository.save(oldContract);
            roomRepository.save(oldRoom);
        } else {
            activateReplacementOldContract(request, oldContract, oldOccupants, remainingProfileIds, executionDate);
            leaseContractRepository.save(oldContract);
        }

        alignTransferContractStart(newContract, executionDate);
        newContract.activateContract();
        targetRoom.occupyRoom();
        markDepositTransferRecordEffective(request, oldContract, newContract, targetRoom, executionDate);
        leaseContractRepository.save(newContract);
        roomRepository.save(targetRoom);
        SettlementType settlementType = Optional.ofNullable(command.positiveDifferenceSettlementType())
                .orElse(request.getPositiveDifferenceSettlementType());
        if (settlementType != SettlementType.TENANT_PAY_MORE) {
            createRentDifferenceSettlement(
                    request,
                    oldContract,
                    newContract,
                    targetRoom,
                    command.executedById(),
                    settlementType
            );
        }
        releaseTargetCapacity(request);
    }

    private void ensureDepositTransferRecordDraft(
            RoomTransferRequest request,
            LeaseContract oldContract,
            LeaseContract newContract,
            Room targetRoom
    ) {
        if (request == null || oldContract == null || newContract == null || targetRoom == null) {
            return;
        }
        TargetTransferType transferType = Optional.ofNullable(request.getTargetTransferType())
                .orElse(TargetTransferType.NEW_CONTRACT);
        if (transferType != TargetTransferType.NEW_CONTRACT) {
            return;
        }

        DepositTransferRecord existing = depositTransferRecordRepository
                .findByTransferRequestId(request.getId())
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == DepositTransferStatus.CANCELLED) {
                return;
            }
            existing.setOldContractId(oldContract.getId());
            existing.setNewContractId(newContract.getId());
            existing.setOldDepositAgreementId(oldContract.getDepositAgreementId());
            existing.setFromRoomId(oldContract.getRoomId());
            existing.setToRoomId(targetRoom.getId());
            existing.setAmount(safe(oldContract.getDepositAmount()));
            existing.setEffectiveDate(request.getRequestedTransferDate());
            existing.setNote(buildDepositTransferNote(oldContract, newContract));
            depositTransferRecordRepository.save(existing);
            return;
        }

        depositTransferRecordRepository.save(DepositTransferRecord.builder()
                .transferRequestId(request.getId())
                .oldContractId(oldContract.getId())
                .newContractId(newContract.getId())
                .oldDepositAgreementId(oldContract.getDepositAgreementId())
                .fromRoomId(oldContract.getRoomId())
                .toRoomId(targetRoom.getId())
                .amount(safe(oldContract.getDepositAmount()))
                .status(DepositTransferStatus.DRAFT)
                .effectiveDate(request.getRequestedTransferDate())
                .note(buildDepositTransferNote(oldContract, newContract))
                .build());
    }

    private void markDepositTransferRecordEffective(
            RoomTransferRequest request,
            LeaseContract oldContract,
            LeaseContract newContract,
            Room targetRoom,
            LocalDate executionDate
    ) {
        ensureDepositTransferRecordDraft(request, oldContract, newContract, targetRoom);
        DepositTransferRecord record = depositTransferRecordRepository
                .findByTransferRequestId(request.getId())
                .orElseThrow(() -> new IllegalStateException("Deposit transfer record is required before executing room transfer."));
        record.markEffective(executionDate);
        depositTransferRecordRepository.save(record);
    }

    private void cancelDepositTransferRecord(RoomTransferRequest request) {
        if (request == null || request.getId() == null) {
            return;
        }
        depositTransferRecordRepository.findByTransferRequestId(request.getId())
                .filter(record -> record.getStatus() == DepositTransferStatus.DRAFT)
                .ifPresent(record -> {
                    record.cancel();
                    depositTransferRecordRepository.save(record);
                });
    }

    private String buildDepositTransferNote(LeaseContract oldContract, LeaseContract newContract) {
        return "Carry over deposit from contract "
                + valueOrBlank(oldContract.getContractCode())
                + " to transfer contract "
                + valueOrBlank(newContract.getContractCode())
                + ".";
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
        if (request.getNewContractId() == null) {
            throw new IllegalStateException("Transfer agreement must be generated before executing existing-contract transfer.");
        }
        if (getContract(request.getNewContractId()).getStatus() != LeaseStatus.SIGNED) {
            throw new IllegalStateException("Transfer agreement must be SIGNED before executing transfer.");
        }
        LocalDate executionDate = submitTransferInHandover(command.transferInHandover(), targetContract.getId(), false);

        moveTransferredOccupantsToExistingContract(request, targetContract, oldOccupants, executionDate);

        if (remainingProfileIds.isEmpty()) {
            oldContract.markTransferred();
            oldRoom.releaseRoom();
            closeSupersededRenewals(oldContract, executionDate);
            leaseContractRepository.save(oldContract);
            roomRepository.save(oldRoom);
        } else {
            activateReplacementOldContract(request, oldContract, oldOccupants, remainingProfileIds, executionDate);
            leaseContractRepository.save(oldContract);
        }

        roomRepository.save(targetRoom);
        releaseTargetCapacity(request);
    }

    private void activateReplacementOldContract(
            RoomTransferRequest request,
            LeaseContract oldContract,
            List<ContractOccupant> oldOccupants,
            List<Long> remainingProfileIds,
            LocalDate executionDate
    ) {
        if (request.getReplacementOldContractId() == null) {
            throw new IllegalStateException("Replacement old-room contract is required before executing holder replacement.");
        }
        LeaseContract replacementContract = getContract(request.getReplacementOldContractId());
        if (replacementContract.getStatus() != LeaseStatus.SIGNED) {
            throw new IllegalStateException("Replacement old-room contract must be SIGNED before executing transfer.");
        }
        moveRemainingOccupantsToReplacementContract(request, replacementContract, oldOccupants, remainingProfileIds, executionDate);
        oldContract.markTransferred();
        alignTransferContractStart(replacementContract, executionDate);
        replacementContract.activateContract();
        leaseContractRepository.save(replacementContract);
    }

    private void moveRemainingOccupantsToReplacementContract(
            RoomTransferRequest request,
            LeaseContract replacementContract,
            List<ContractOccupant> oldOccupants,
            List<Long> remainingProfileIds,
            LocalDate executionDate
    ) {
        Set<Long> remainingIds = new HashSet<>(remainingProfileIds);
        List<ContractOccupant> replacementOccupants = new ArrayList<>();
        for (ContractOccupant oldOccupant : oldOccupants) {
            Long profileId = oldOccupant.getTenantProfileId();
            if (!remainingIds.contains(profileId)) {
                continue;
            }
            oldOccupant.moveOut(executionDate);
            replacementOccupants.add(ContractOccupant.builder()
                    .contractId(replacementContract.getId())
                    .tenantId(oldOccupant.getTenantId())
                    .tenantProfileId(profileId)
                    .occupantRole(profileId.equals(replacementContract.getPrimaryTenantProfileId())
                            ? OccupantRole.PRIMARY
                            : OccupantRole.CO_OCCUPANT)
                    .moveInDate(executionDate)
                    .status(OccupantStatus.ACTIVE)
                    .build());
        }
        contractOccupantRepository.saveAll(oldOccupants);
        contractOccupantRepository.saveAll(replacementOccupants);
    }

    private void closeSupersededRenewals(LeaseContract rootContract, LocalDate executionDate) {
        if (rootContract == null || rootContract.getId() == null) {
            return;
        }
        List<Long> renewalIds = jdbcTemplate.query("""
                        WITH RECURSIVE contract_chain AS (
                            SELECT lc.lease_contract_id, lc.status
                            FROM lease_contracts lc
                            WHERE lc.previous_contract_id = ?
                              AND lc.room_id = ?
                              AND lc.deleted_at IS NULL
                            UNION ALL
                            SELECT child.lease_contract_id, child.status
                            FROM lease_contracts child
                            JOIN contract_chain parent ON child.previous_contract_id = parent.lease_contract_id
                            WHERE child.room_id = ?
                              AND child.deleted_at IS NULL
                        )
                        SELECT lease_contract_id
                        FROM contract_chain
                        WHERE status IN ('DRAFT', 'PENDING_SIGNATURE', 'CONFIRMED', 'SIGNED', 'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING')
                        """,
                (rs, rowNum) -> rs.getLong("lease_contract_id"),
                rootContract.getId(),
                rootContract.getRoomId(),
                rootContract.getRoomId()
        );
        for (Long renewalId : renewalIds) {
            leaseContractRepository.findById(renewalId).ifPresent(renewal -> {
                LeaseStatus status = renewal.getStatus();
                if (status == LeaseStatus.ACTIVE
                        || status == LeaseStatus.EXPIRING_SOON
                        || status == LeaseStatus.TERMINATION_PENDING) {
                    renewal.markTransferred();
                } else {
                    renewal.cancelContract();
                }
                List<ContractOccupant> occupants = activeOccupants(renewal.getId());
                for (ContractOccupant occupant : occupants) {
                    occupant.moveOut(executionDate);
                }
                contractOccupantRepository.saveAll(occupants);
                leaseContractRepository.save(renewal);
            });
        }
    }

    private void moveTransferredOccupantsToExistingContract(
            RoomTransferRequest request,
            LeaseContract targetContract,
            List<ContractOccupant> oldOccupants,
            LocalDate executionDate
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
            oldOccupant.moveOut(executionDate);
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
                    .moveInDate(executionDate)
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
        if (targetRoom.getCurrentStatus() == RoomStatus.SOON_VACANT && currentDestinationOccupancy > 0) {
            LocalDate availableDate = leaseContractRepository
                    .findFirstActiveContract(targetRoom.getId(), DESTINATION_BLOCKING_CONTRACT_STATUSES)
                    .map(entity -> entity.getExpectedVacantDate() == null ? entity.getEndDate() : entity.getExpectedVacantDate())
                    .orElse(LocalDate.now());
            if (requestedTransferDate.isBefore(availableDate)) {
                throw new IllegalArgumentException("Expected transfer date is before target room available date.");
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

    private List<Long> normalizeTransferredProfiles(
            List<Long> requestedProfileIds,
            Long requesterProfileId
    ) {
        if (requestedProfileIds == null || requestedProfileIds.isEmpty()) {
            return List.of(requesterProfileId);
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

    private void requireTransferringTenant(RoomTransferRequest request, Long tenantUserId) {
        PersonProfile tenantProfile = personProfileRepository.findByUserId(tenantUserId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        if (request.getTransferringTenantProfileIds() == null
                || !request.getTransferringTenantProfileIds().contains(tenantProfile.getId())) {
            throw new IllegalArgumentException("Only transferring tenant can perform this transfer action.");
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

    private void applyHolderNominationFromTenantConfirmation(
            ConfirmTenantTransferCommand command,
            RoomTransferRequest request,
            LeaseContract oldContract,
            List<ContractOccupant> activeOccupants
    ) {
        if (!holderNominationRequired(oldContract, request, activeOccupants)) {
            return;
        }
        Long nominatedHolderProfileId = request.getNominatedHolderProfileId();
        if (nominatedHolderProfileId == null) {
            throw new IllegalStateException("Holder nomination must be accepted before confirming this transfer.");
        }
        if (!remainingProfileIds(request, activeOccupants).contains(nominatedHolderProfileId)) {
            throw new IllegalStateException("Nominated holder must be one of the remaining occupants.");
        }
    }

    private void requireHolderNominationOpenStatus(RoomTransferRequest request) {
        if (request.getStatus() != TransferRequestStatus.MANAGER_APPROVED
                && request.getStatus() != TransferRequestStatus.WAITING_NEW_CONTRACT) {
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
            List<ContractOccupant> oldOccupants,
            LocalDate executionDate
    ) {
        Set<Long> transferringIds = new HashSet<>(request.getTransferringTenantProfileIds());
        List<ContractOccupant> newOccupants = new ArrayList<>();
        for (ContractOccupant oldOccupant : oldOccupants) {
            Long profileId = oldOccupant.getTenantProfileId();
            if (!transferringIds.contains(profileId)) {
                continue;
            }
            oldOccupant.moveOut(executionDate);

            newOccupants.add(ContractOccupant.builder()
                    .contractId(newContract.getId())
                    .tenantId(oldOccupant.getTenantId())
                    .tenantProfileId(oldOccupant.getTenantProfileId())
                    .occupantRole(profileId.equals(newContract.getPrimaryTenantProfileId())
                            ? OccupantRole.PRIMARY
                            : OccupantRole.CO_OCCUPANT)
                    .moveInDate(executionDate)
                    .status(OccupantStatus.ACTIVE)
                    .build());
        }
        contractOccupantRepository.saveAll(oldOccupants);
        contractOccupantRepository.saveAll(newOccupants);
    }

    private void alignTransferContractStart(LeaseContract contract, LocalDate executionDate) {
        contract.setStartDate(executionDate);
        contract.setRentStartDate(executionDate);
    }

    private List<ContractOccupant> activeOccupants(Long contractId) {
        return contractOccupantRepository.findAllByContractIdAndStatus(contractId, OccupantStatus.ACTIVE);
    }

    private record TransferRentDifference(
            long oldRoomRemainingValue,
            long newRoomRequiredValue,
            long differenceAmount,
            int chargeableRemainingMonths
    ) {}

    private record DebtSnapshotDetails(
            long rentDebtAmount,
            long utilityDebtAmount,
            long otherDebtAmount,
            int rentDebtMonths,
            int utilityDebtMonths,
            long totalDebtAmount,
            Long debtLimitAmount,
            boolean overLimit
    ) {}

    private TransferRentDifference calculateTransferRentDifference(
            LeaseContract oldContract,
            LeaseContract newContract,
            Room targetRoom,
            LocalDate requestedTransferDate
    ) {
        long oldRent = safe(oldContract.getMonthlyRent());
        long newRent = newContract != null ? safe(newContract.getMonthlyRent()) : safe(targetRoom.getListedPrice());
        int paymentCycleMonths = oldContract.getPaymentCycleMonths() == null
                ? 1
                : Math.max(1, oldContract.getPaymentCycleMonths());

        if (paymentCycleMonths <= 1) {
            return new TransferRentDifference(0L, 0L, 0L, 0);
        }

        LocalDate transferDate = requestedTransferDate == null ? LocalDate.now() : requestedTransferDate;
        LocalDate cycleStart = Optional.ofNullable(oldContract.getRentStartDate())
                .or(() -> Optional.ofNullable(oldContract.getStartDate()))
                .orElse(transferDate.withDayOfMonth(1));
        while (!transferDate.isBefore(cycleStart.plusMonths(paymentCycleMonths))) {
            cycleStart = cycleStart.plusMonths(paymentCycleMonths);
        }
        while (transferDate.isBefore(cycleStart)) {
            cycleStart = cycleStart.minusMonths(paymentCycleMonths);
        }

        LocalDate cycleEndExclusive = cycleStart.plusMonths(paymentCycleMonths);
        LocalDate chargeStart = transferDate.getDayOfMonth() >= 11
                ? transferDate.plusMonths(1).withDayOfMonth(1)
                : transferDate.withDayOfMonth(1);
        LocalDate cycleStartMonth = cycleStart.withDayOfMonth(1);
        if (chargeStart.isBefore(cycleStartMonth)) {
            chargeStart = cycleStartMonth;
        }
        if (!chargeStart.isBefore(cycleEndExclusive)) {
            return new TransferRentDifference(0L, 0L, 0L, 0);
        }

        int chargeableMonths = Math.max(
                0,
                (int) java.time.temporal.ChronoUnit.MONTHS.between(
                        YearMonth.from(chargeStart),
                        YearMonth.from(cycleEndExclusive)
                )
        );
        long oldRoomRemainingValue = oldRent * chargeableMonths;
        long newRoomRequiredValue = newRent * chargeableMonths;
        return new TransferRentDifference(
                oldRoomRemainingValue,
                newRoomRequiredValue,
                newRoomRequiredValue - oldRoomRemainingValue,
                chargeableMonths
        );
    }

    private void createRentDifferenceSettlement(
            RoomTransferRequest request,
            LeaseContract oldContract,
            LeaseContract newContract,
            Room targetRoom,
            Long executorId,
            SettlementType positiveDifferenceSettlementType
    ) {
        TransferRentDifference rentDifference = calculateTransferRentDifference(
                oldContract,
                newContract,
                targetRoom,
                request.getRequestedTransferDate()
        );
        long oldRoomRemainingValue = rentDifference.oldRoomRemainingValue();
        long newRoomRequiredValue = rentDifference.newRoomRequiredValue();
        long difference = rentDifference.differenceAmount();

        if (difference > 0) {
            SettlementType settlementType = Optional.ofNullable(positiveDifferenceSettlementType)
                    .orElse(SettlementType.TENANT_PAY_MORE);
            if (settlementType == SettlementType.ADD_TO_NEXT_INVOICE) {
                if (newContract == null) {
                    throw new IllegalStateException("Target contract is required to add transfer difference to the next invoice.");
                }
                Invoice invoice = createNextRentInvoiceWithTransferSurcharge(
                        request,
                        newContract,
                        targetRoom,
                        difference,
                        executorId
                );
                // transfer_settlements.settlement_type in current schema does not support
                // ADD_TO_NEXT_INVOICE. Persist legacy-compatible positive-difference marker
                // and rely on linked invoice plus request.positiveDifferenceSettlementType
                // to distinguish "add to next invoice" from "pay now".
                saveSettlement(
                        request,
                        oldRoomRemainingValue,
                        newRoomRequiredValue,
                        difference,
                        SettlementType.TENANT_PAY_MORE,
                        invoice.getId(),
                        executorId
                );
            } else {
                // New room is more expensive — tenant owes the difference immediately.
                // Invoice must stay visible to current tenant before transfer executes,
                // so bind it to old/current contract instead of target contract.
                Invoice invoice = createTransferDifferenceInvoice(
                        request,
                        oldContract,
                        oldRoomRemainingValue,
                        newRoomRequiredValue,
                        difference,
                        targetRoom,
                        executorId
                );
                saveSettlement(request, oldRoomRemainingValue, newRoomRequiredValue, difference, SettlementType.TENANT_PAY_MORE, invoice.getId(), executorId);
            }
        } else if (difference < 0) {
            // New room is cheaper — tenant gets credit applied to next month's rent
            if (newContract == null) {
                throw new IllegalStateException("Target contract is required to apply transfer credit to the next invoice.");
            }
            long credit = Math.abs(difference);
            createNextRentInvoiceWithTransferCredit(request, newContract, targetRoom, credit, executorId);
            saveSettlement(request, oldRoomRemainingValue, newRoomRequiredValue, difference, SettlementType.CREDIT_NEXT_CONTRACT, null, executorId);
        } else {
            // Same rent — no settlement needed
            saveSettlement(request, oldRoomRemainingValue, newRoomRequiredValue, 0L, SettlementType.NO_DIFFERENCE, null, executorId);
        }
    }

    private SubmitHandoverResponse submitTransferOutHandover(
            ExecuteTransferCommand command,
            Long oldContractId,
            boolean sourceRoomWillBeEmpty
    ) {
        ExecuteTransferCommand.TransferHandoverData payload = command.transferOutHandover();
        if (payload == null) {
            return requireConfirmedHandover(
                    oldContractId,
                    HandoverType.TRANSFER_OUT,
                    "Transfer-out handover must be saved before executing transfer."
            );
        }
        validateTransferHandoverDate(payload, "transfer-out");
        if (sourceRoomWillBeEmpty && (payload.assets() == null || payload.assets().isEmpty())) {
            throw new IllegalArgumentException("Room handover assets are required when the source room becomes empty.");
        }
        return manageContractHandoverService.submitHandover(
                oldContractId,
                toSubmitHandoverRequest(payload, HandoverType.TRANSFER_OUT)
        );
    }

    private LocalDate submitTransferInHandover(
            ExecuteTransferCommand.TransferHandoverData payload,
            Long targetContractId,
            boolean required
    ) {
        if (payload == null) {
            if (required) {
                SubmitHandoverResponse handover = requireConfirmedHandover(
                        targetContractId,
                        HandoverType.TRANSFER_IN,
                        "Transfer-in handover must be saved before completing transfer."
                );
                return handover.getHandoverDate() == null ? LocalDate.now() : handover.getHandoverDate().toLocalDate();
            }
            return LocalDate.now();
        }
        validateTransferHandoverDate(payload, "transfer-in");
        manageContractHandoverService.submitHandover(
                targetContractId,
                toSubmitHandoverRequest(payload, HandoverType.TRANSFER_IN)
        );
        return payload.handoverDate();
    }

    private SubmitHandoverResponse requireConfirmedHandover(
            Long contractId,
            HandoverType handoverType,
            String missingMessage
    ) {
        ContractHandoverDetailsResponse handover;
        try {
            handover = manageContractHandoverService.getHandoverDetails(contractId, handoverType);
        } catch (AppException exception) {
            throw new IllegalStateException(missingMessage);
        }
        if (handover.getStatus() != HandoverStatus.CONFIRMED) {
            throw new IllegalStateException(missingMessage);
        }
        Long electricityReadingId = handover.getElectricity() == null ? null : handover.getElectricity().getId();
        Long waterReadingId = handover.getWater() == null ? null : handover.getWater().getId();
        if (electricityReadingId == null || waterReadingId == null) {
            throw new IllegalStateException(missingMessage);
        }
        return SubmitHandoverResponse.builder()
                .handoverRecordId(handover.getHandoverRecordId())
                .handoverType(handover.getHandoverType())
                .status(handover.getStatus())
                .handoverDate(handover.getHandoverDate())
                .electricityReadingId(electricityReadingId)
                .waterReadingId(waterReadingId)
                .build();
    }

    private int nextInvoiceRevision(Long contractId, String billingPeriod, InvoiceType invoiceType) {
        Integer maxRevision = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(revision_no), 0)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND billing_period = ?
                          AND invoice_type = ?
                        """,
                Integer.class,
                contractId,
                billingPeriod,
                invoiceType.name()
        );
        return (maxRevision == null ? 0 : maxRevision) + 1;
    }

    private TransferOutUtilityEstimateResponse.MeterChargeEstimate estimateMeterCharge(
            Long roomId,
            Long propertyId,
            UtilityType utilityType,
            ExecuteTransferCommand.MeterReadingData input
    ) {
        if (input == null || input.currentValue() == null) {
            throw new IllegalArgumentException(utilityType + " current reading is required.");
        }
        LocalDate readingDate = input.readingDate() == null ? LocalDate.now() : input.readingDate();
        BigDecimal previousValue = readLatestRoomReading(roomId, utilityType);
        BigDecimal currentValue = input.currentValue();
        BigDecimal usage = currentValue.subtract(previousValue);
        if (usage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(utilityType + " current reading must be greater than or equal to previous reading.");
        }

        UtilityTariffSnapshot tariff = readUtilityTariff(propertyId, utilityType, readingDate);
        BigDecimal billableUsage = usage.subtract(BigDecimal.valueOf(tariff.freeAllowance()));
        int quantity = billableUsage.compareTo(BigDecimal.ZERO) <= 0
                ? 0
                : billableUsage.setScale(0, RoundingMode.CEILING).intValueExact();
        long amount = quantity * tariff.unitPrice();
        return new TransferOutUtilityEstimateResponse.MeterChargeEstimate(
                previousValue,
                currentValue,
                usage,
                tariff.freeAllowance(),
                quantity,
                tariff.unitPrice(),
                amount
        );
    }

    private BigDecimal readLatestRoomReading(Long roomId, UtilityType utilityType) {
        return jdbcTemplate.query("""
                        SELECT reading.current_value
                        FROM meter_readings reading
                        JOIN meters meter ON meter.meter_id = reading.meter_id
                        WHERE reading.room_id = ?
                          AND meter.meter_type = ?
                          AND reading.status <> 'VOIDED'
                        ORDER BY reading.reading_date DESC, reading.created_at DESC, reading.meter_reading_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getBigDecimal("current_value"),
                roomId,
                utilityType.name()
        ).stream().findFirst().orElse(BigDecimal.ZERO);
    }

    private ServiceFeeCharge buildTransferServiceFeeCharge(
            Long oldContractId,
            Long propertyId,
            String billingPeriod,
            LocalDate readingDate,
            long electricityAmount
    ) {
        if (hasServiceFeeLineForContractAndPeriod(oldContractId, billingPeriod)) {
            return ServiceFeeCharge.empty();
        }
        UtilityTariffSnapshot tariff = readUtilityTariff(propertyId, UtilityType.SERVICE_FEE, readingDate);
        Long waiveThreshold = tariff.serviceFeeWaiveElectricityThreshold();
        if (waiveThreshold != null && electricityAmount < waiveThreshold) {
            return new ServiceFeeCharge(
                    tariff.unitPrice(),
                    0L,
                    "Service fee waived because electricity amount is below " + waiveThreshold + ".",
                    true
            );
        }
        return new ServiceFeeCharge(
                tariff.unitPrice(),
                tariff.unitPrice(),
                "Service fee " + billingPeriod,
                true
        );
    }

    private boolean hasServiceFeeLineForContractAndPeriod(Long contractId, String billingPeriod) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM invoices invoice
                JOIN invoice_lines line ON line.invoice_id = invoice.invoice_id
                WHERE invoice.lease_contract_id = ?
                  AND invoice.billing_period = ?
                  AND invoice.status <> 'VOIDED'
                  AND line.line_type = 'SERVICE_FEE'
                """, Integer.class, contractId, billingPeriod);
        return count != null && count > 0;
    }

    private UtilityTariffSnapshot readUtilityTariff(Long propertyId, UtilityType utilityType, LocalDate readingDate) {
        Optional<UtilityTariffSnapshot> propertyTariff = jdbcTemplate.query("""
                        SELECT unit_price, free_allowance, service_fee_waive_electricity_threshold
                        FROM utility_tariffs
                        WHERE property_id = ?
                          AND utility_type = ?
                          AND effective_from <= ?
                          AND (effective_to IS NULL OR effective_to >= ?)
                        ORDER BY effective_from DESC, utility_tariff_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new UtilityTariffSnapshot(
                        rs.getLong("unit_price"),
                        rs.getLong("free_allowance"),
                        rs.getObject("service_fee_waive_electricity_threshold") == null
                                ? null
                                : rs.getLong("service_fee_waive_electricity_threshold")
                ),
                propertyId,
                utilityType.name(),
                readingDate,
                readingDate
        ).stream().findFirst();
        if (propertyTariff.isPresent()) {
            return propertyTariff.get();
        }

        // TODO ponytail: Temporary dev fallback until owner-managed utility tariffs are required before transfer checkout.
        return switch (utilityType) {
            case ELECTRICITY -> new UtilityTariffSnapshot(3500L, 0L, null);
            case WATER -> new UtilityTariffSnapshot(20000L, 6L, null);
            case SERVICE_FEE -> new UtilityTariffSnapshot(50000L, 0L, 100000L);
        };
    }

    private void requireTransferOutUtilityInvoicePaid(RoomTransferRequest request) {
        TransferSettlement settlement = transferSettlementRepository.findLatestByTransferRequestId(request.getId()).orElse(null);
        if (settlement != null && settlement.getOldRoomFinalInvoiceId() != null) {
            requirePaidInvoice(
                    settlement.getOldRoomFinalInvoiceId(),
                    "Hóa đơn điện nước chốt phòng cũ #" + settlement.getOldRoomFinalInvoiceId()
                            + " cần được thanh toán trước khi hoàn tất chuyển phòng."
            );
        }

        for (Long invoiceId : findOldRoomCompensationInvoiceIds(request)) {
            requirePaidInvoice(
                    invoiceId,
                    "Hóa đơn bồi thường phòng cũ #" + invoiceId
                            + " cần được thanh toán trước khi hoàn tất chuyển phòng."
            );
        }
    }

    private void requirePaidInvoice(Long invoiceId, String unpaidMessage) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy hóa đơn chốt phòng cũ #" + invoiceId + "."));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new IllegalStateException(unpaidMessage);
        }
    }

    private void createOldRoomCompensationInvoiceIfNeeded(
            RoomTransferRequest request,
            LeaseContract oldContract,
            Room oldRoom,
            Long amount,
            String note,
            Long managerId
    ) {
        long compensationAmount = safe(amount);
        if (compensationAmount <= 0) {
            return;
        }
        List<Long> existingInvoiceIds = findOldRoomCompensationInvoiceIds(request);
        if (!existingInvoiceIds.isEmpty()) {
            return;
        }

        String billingPeriod = YearMonth.from(request.getRequestedTransferDate()).toString();
        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceCode("INV-TR-COMP-" + request.getId() + "-" + snowflakeIdGenerator.next())
                .propertyId(oldRoom.getPropertyId())
                .roomId(oldRoom.getId())
                .leaseContractId(oldContract.getId())
                .invoiceType(InvoiceType.FINAL_SETTLEMENT)
                .invoiceReason(InvoiceReason.ROOM_CLOSE)
                .revisionNo(nextInvoiceRevision(oldContract.getId(), billingPeriod, InvoiceType.FINAL_SETTLEMENT))
                .billingPeriod(billingPeriod)
                .issueDate(now)
                .dueDate(now.plusDays(7))
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(compensationAmount)
                .discountAmount(0L)
                .totalAmount(compensationAmount)
                .paidAmount(0L)
                .remainingAmount(compensationAmount)
                .createdBy(managerId)
                .build());
        invoiceLineRepository.save(InvoiceLine.builder()
                .invoiceId(invoice.getId())
                .lineType(InvoiceLineType.MANUAL_ADJUSTMENT)
                .description(buildOldRoomCompensationDescription(note))
                .quantity(1)
                .unitPrice(compensationAmount)
                .sourceType(SOURCE_ROOM_TRANSFER_COMPENSATION)
                .sourceId(request.getId())
                .build());
        issuedInvoiceChargeService.issueDraftInvoice(invoice.getId());
    }

    private List<Long> findOldRoomCompensationInvoiceIds(RoomTransferRequest request) {
        if (request == null || request.getId() == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                        SELECT DISTINCT invoice.invoice_id
                        FROM invoices invoice
                        JOIN invoice_lines line ON line.invoice_id = invoice.invoice_id
                        WHERE line.source_type = ?
                          AND line.source_id = ?
                          AND line.line_type = ?
                          AND invoice.status <> 'VOIDED'
                        ORDER BY invoice.invoice_id DESC
                        """,
                (rs, rowNum) -> rs.getLong("invoice_id"),
                SOURCE_ROOM_TRANSFER_COMPENSATION,
                request.getId(),
                InvoiceLineType.MANUAL_ADJUSTMENT.name()
        );
    }

    private String buildOldRoomCompensationDescription(String note) {
        String trimmedNote = note == null ? "" : note.trim();
        String description = trimmedNote.isBlank()
                ? "Bồi thường khi bàn giao phòng cũ"
                : "Bồi thường khi bàn giao phòng cũ: " + trimmedNote;
        return description.length() <= 1000 ? description : description.substring(0, 1000);
    }

    private record UtilityTariffSnapshot(long unitPrice, long freeAllowance, Long serviceFeeWaiveElectricityThreshold) {}

    private record ServiceFeeCharge(long unitPrice, long amount, String description, boolean lineRequired) {
        static ServiceFeeCharge empty() {
            return new ServiceFeeCharge(0L, 0L, null, false);
        }
    }

    private void saveOldRoomFinalInvoiceSettlement(
            RoomTransferRequest request,
            Long oldRoomFinalInvoiceId,
            Long managerId
    ) {
        TransferSettlement existing = transferSettlementRepository.findLatestByTransferRequestId(request.getId()).orElse(null);
        transferSettlementRepository.save(TransferSettlement.builder()
                .id(existing == null ? null : existing.getId())
                .transferRequestId(request.getId())
                .oldRoomRemainingValue(existing == null ? 0L : safe(existing.getOldRoomRemainingValue()))
                .newRoomRequiredValue(existing == null ? 0L : safe(existing.getNewRoomRequiredValue()))
                .differenceAmount(existing == null ? 0L : safe(existing.getDifferenceAmount()))
                .settlementType(existing == null || existing.getSettlementType() == null
                        ? SettlementType.NO_DIFFERENCE
                        : existing.getSettlementType())
                .oldRoomFinalInvoiceId(oldRoomFinalInvoiceId)
                .transferDifferenceInvoiceId(existing == null ? null : existing.getTransferDifferenceInvoiceId())
                .confirmedById(existing != null && existing.getConfirmedById() != null ? existing.getConfirmedById() : managerId)
                .confirmedAt(existing != null && existing.getConfirmedAt() != null ? existing.getConfirmedAt() : LocalDateTime.now())
                .build());
    }

    private void validateTransferHandoverDate(
            ExecuteTransferCommand.TransferHandoverData payload,
            String handoverName
    ) {
        LocalDate handoverDate = payload.handoverDate();
        if (handoverDate == null) {
            throw new IllegalArgumentException("Transfer handover date is required.");
        }
        validateDateNotFuture(handoverDate, handoverName + " handover");
        validateMeterReadingDate(payload.electricity(), handoverName + " electricity reading");
        validateMeterReadingDate(payload.water(), handoverName + " water reading");
    }

    private void validateMeterReadingDate(
            ExecuteTransferCommand.MeterReadingData reading,
            String readingName
    ) {
        if (reading == null || reading.readingDate() == null) {
            return;
        }
        validateDateNotFuture(reading.readingDate(), readingName);
    }

    private void validateDateNotFuture(LocalDate date, String fieldName) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " must not be in the future.");
        }
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
            LeaseContract oldContract,
            long oldRent,
            long newRent,
            long amount,
            Room targetRoom,
            Long managerId
    ) {
        String billingPeriod = YearMonth.from(request.getRequestedTransferDate()).toString();
        Optional<Long> existingInvoiceId = jdbcTemplate.query("""
                        SELECT invoice.invoice_id
                        FROM invoices invoice
                        JOIN invoice_lines line ON line.invoice_id = invoice.invoice_id
                        WHERE invoice.lease_contract_id = ?
                          AND invoice.billing_period = ?
                          AND invoice.invoice_type = ?
                          AND line.source_type = 'ROOM_TRANSFER'
                          AND line.source_id = ?
                          AND line.line_type = ?
                          AND invoice.status <> 'VOIDED'
                        ORDER BY invoice.revision_no DESC, invoice.invoice_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong("invoice_id"),
                oldContract.getId(),
                billingPeriod,
                InvoiceType.TRANSFER_DIFFERENCE.name(),
                request.getId(),
                InvoiceLineType.TRANSFER_DIFFERENCE.name()
        ).stream().findFirst();

        if (existingInvoiceId.isPresent()) {
            return invoiceRepository.findById(existingInvoiceId.get())
                    .orElseThrow(() -> new IllegalStateException("Existing transfer difference invoice could not be loaded."));
        }

        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceCode("INV-TR-" + request.getId() + "-" + snowflakeIdGenerator.next())
                .propertyId(targetRoom.getPropertyId())
                .roomId(targetRoom.getId())
                .leaseContractId(oldContract.getId())
                .invoiceType(InvoiceType.TRANSFER_DIFFERENCE)
                .revisionNo(nextInvoiceRevision(oldContract.getId(), billingPeriod, InvoiceType.TRANSFER_DIFFERENCE))
                .billingPeriod(billingPeriod)
                .issueDate(now)
                .dueDate(now.plusDays(7))
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(amount)
                .discountAmount(0L)
                .totalAmount(amount)
                .paidAmount(0L)
                .remainingAmount(amount)
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
        issuedInvoiceChargeService.issueDraftInvoice(invoice.getId());
        return invoiceRepository.findById(invoice.getId())
                .orElseThrow(() -> new IllegalStateException("Transfer difference invoice could not be issued."));
    }

    private Invoice createNextRentInvoiceWithTransferSurcharge(
            RoomTransferRequest request,
            LeaseContract newContract,
            Room targetRoom,
            long surchargeAmount,
            Long managerId
    ) {
        YearMonth nextBillingPeriod = YearMonth.from(request.getRequestedTransferDate()).plusMonths(1);
        LocalDateTime issueDate = nextBillingPeriod.atDay(1).atTime(8, 0);
        LocalDateTime dueDate = nextBillingPeriod.atDay(5).atTime(23, 59, 59);
        long rentAmount = safe(newContract.getMonthlyRent());
        return invoiceRepository
                .findFirstByLeastContractIdAndBillingPeriodAndInvoiceTypeAndStatusOrderByIdDesc(
                        newContract.getId(),
                        nextBillingPeriod.toString(),
                        InvoiceType.RENT,
                        InvoiceStatus.DRAFT
                )
                .map(invoice -> applySurchargeToExistingRentInvoice(invoice, request.getId(), surchargeAmount))
                .orElseGet(() -> createSurchargedRentInvoice(
                        request,
                        newContract,
                        targetRoom,
                        nextBillingPeriod,
                        issueDate,
                        dueDate,
                        rentAmount,
                        surchargeAmount,
                        managerId
                ));
    }

    private Invoice createSurchargedRentInvoice(
            RoomTransferRequest request,
            LeaseContract newContract,
            Room targetRoom,
            YearMonth nextBillingPeriod,
            LocalDateTime issueDate,
            LocalDateTime dueDate,
            long rentAmount,
            long surchargeAmount,
            Long managerId
    ) {
        long totalAmount = rentAmount + surchargeAmount;
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceCode("INV-RENT-TRS-" + request.getId() + "-" + nextBillingPeriod.toString().replace("-", ""))
                .propertyId(targetRoom.getPropertyId())
                .roomId(targetRoom.getId())
                .leaseContractId(newContract.getId())
                .invoiceType(InvoiceType.RENT)
                .billingPeriod(nextBillingPeriod.toString())
                .issueDate(issueDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.DRAFT)
                .subtotalAmount(totalAmount)
                .discountAmount(0L)
                .totalAmount(totalAmount)
                .paidAmount(0L)
                .remainingAmount(totalAmount)
                .createdBy(managerId)
                .build());
        invoiceLineRepository.save(InvoiceLine.builder()
                .invoiceId(invoice.getId())
                .lineType(InvoiceLineType.ROOM_RENT)
                .description("Room rent")
                .quantity(1)
                .unitPrice(rentAmount)
                .sourceType("ROOM_TRANSFER")
                .sourceId(request.getId())
                .build());
        invoiceLineRepository.save(InvoiceLine.builder()
                .invoiceId(invoice.getId())
                .lineType(InvoiceLineType.TRANSFER_DIFFERENCE)
                .description("Room transfer rent difference added to next invoice")
                .quantity(1)
                .unitPrice(surchargeAmount)
                .sourceType("ROOM_TRANSFER_SURCHARGE")
                .sourceId(request.getId())
                .build());
        return invoice;
    }

    private Invoice applySurchargeToExistingRentInvoice(Invoice invoice, Long requestId, long surchargeAmount) {
        invoice.addSurchargeAmount(surchargeAmount);
        Invoice saved = invoiceRepository.save(invoice);
        invoiceLineRepository.save(InvoiceLine.builder()
                .invoiceId(saved.getId())
                .lineType(InvoiceLineType.TRANSFER_DIFFERENCE)
                .description("Room transfer rent difference added to next invoice")
                .quantity(1)
                .unitPrice(surchargeAmount)
                .sourceType("ROOM_TRANSFER_SURCHARGE")
                .sourceId(requestId)
                .build());
        return saved;
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
        TransferSettlement existing = transferSettlementRepository.findLatestByTransferRequestId(request.getId()).orElse(null);
        transferSettlementRepository.save(TransferSettlement.builder()
                .id(existing == null ? null : existing.getId())
                .transferRequestId(request.getId())
                .oldRoomRemainingValue(oldRent)
                .newRoomRequiredValue(newRent)
                .differenceAmount(difference)
                .settlementType(settlementType)
                .oldRoomFinalInvoiceId(existing == null ? null : existing.getOldRoomFinalInvoiceId())
                .transferDifferenceInvoiceId(transferDifferenceInvoiceId != null
                        ? transferDifferenceInvoiceId
                        : existing == null ? null : existing.getTransferDifferenceInvoiceId())
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

    private long safeNumber(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private int safeInteger(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private RoomTransferRequest getTransfer(Long requestId) {
        RoomTransferRequest request = roomTransferRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND));
        return syncPaidTransferDifferenceStatus(request);
    }

    private LeaseContract getContract(Long contractId) {
        return leaseContractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(ApiErrorCode.CONTRACT_NOT_FOUND));
    }

    private void requireStatus(RoomTransferRequest request, TransferRequestStatus... expectedStatuses) {
        if (Arrays.stream(expectedStatuses).noneMatch(s -> s == request.getStatus())) {
            throw new IllegalStateException(String.format("Transfer request must be one of %s but was %s",
                    Arrays.toString(expectedStatuses), request.getStatus()));
        }
    }

    private RoomTransferRequest syncPaidTransferDifferenceStatus(RoomTransferRequest request) {
        if (request.getStatus() == TransferRequestStatus.WAITING_TENANT_CONFIRMATION
                && request.getPositiveDifferenceSettlementType() == SettlementType.TENANT_PAY_MORE) {
            Optional<TransferSettlement> paidSettlement = findPaidTransferDifferenceSettlement(request);
            if (paidSettlement.isEmpty()) return request;
            return autoConfirmPaidTransferContract(request, paidSettlement.get());
        }

        if (request.getStatus() == TransferRequestStatus.WAITING_PAYMENT) {
            Optional<TransferSettlement> paidSettlement = findPaidTransferDifferenceSettlement(request);
            if (paidSettlement.isEmpty()) return request;
            return autoConfirmPaidTransferContract(request, paidSettlement.get());
        }

        if (request.getStatus() == TransferRequestStatus.WAITING_CONTRACT_CONFIRMATION) {
            Optional<TransferSettlement> paidSettlement = findPaidTransferDifferenceSettlement(request);
            if (paidSettlement.isPresent()) {
                return autoConfirmPaidTransferContract(request, paidSettlement.get());
            }
        }

        if (request.getStatus() == TransferRequestStatus.WAITING_TRANSFER_DATE) {
            request.setStatus(TransferRequestStatus.READY_FOR_HANDOVER);
            request.setReservationExpiresAt(null);
            request = roomTransferRepository.save(request);
            publishManagerActionRequired(request, ACTION_READY_FOR_HANDOVER);
            return request;
        }

        return request;
    }

    private Optional<TransferSettlement> findPaidTransferDifferenceSettlement(RoomTransferRequest request) {
        TransferSettlement settlement = transferSettlementRepository.findLatestByTransferRequestId(request.getId())
                .orElse(null);
        if (settlement == null || settlement.getTransferDifferenceInvoiceId() == null) {
            return Optional.empty();
        }

        Invoice invoice = invoiceRepository.findById(settlement.getTransferDifferenceInvoiceId()).orElse(null);
        if (invoice == null || invoice.getStatus() != InvoiceStatus.PAID) {
            return Optional.empty();
        }
        return Optional.of(settlement);
    }

    private RoomTransferRequest autoConfirmPaidTransferContract(
            RoomTransferRequest request,
            TransferSettlement settlement
    ) {
        if (settlement.getConfirmedById() == null) {
            return request;
        }
        try {
            return confirmTransferContractForTenant(request, settlement.getConfirmedById());
        } catch (RuntimeException exception) {
            log.warn("Could not auto-confirm room transfer contract after paid difference. transferRequestId={}",
                    request.getId(), exception);
            return request;
        }
    }

    private String nextTransferCode() {
        return "TR-" + snowflakeIdGenerator.next();
    }

    private String nextChangeRequestCode() {
        return "CR-" + snowflakeIdGenerator.next();
    }

    @Override
    @Transactional
    public RoomTransferRequest getTransferRequestById(Long requestId) {
        log.info("Getting transfer request by ID: {}", requestId);
        RoomTransferRequest request = roomTransferRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND));
        return syncPaidTransferDifferenceStatus(request);
    }

    @Override
    @Transactional
    public RoomTransferRequest getTransferRequestByCode(String requestCode) {
        log.info("Getting transfer request by code: {}", requestCode);
        RoomTransferRequest request = roomTransferRepository.findByRequestCode(requestCode)
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_TRANSFER_REQUEST_NOT_FOUND));
        return syncPaidTransferDifferenceStatus(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomTransferRequest> getTransferHistory(
            TransferRequestStatus status,
            Long floorId,
            Long roomId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        return roomTransferRepository.findHistory(status, floorId, roomId, fromDate, toDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTransferRequest> getPendingHolderNominations(Long holderUserId) {
        log.info("Getting pending holder nominations for user: {}", holderUserId);
        return roomTransferRepository.findPendingHolderNominations(holderUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTransferRequest> getPendingTargetHolderApprovals(Long holderUserId) {
        log.info("Getting pending target holder approvals for user: {}", holderUserId);
        return roomTransferRepository.findPendingTargetHolderApprovals(holderUserId);
    }
}
