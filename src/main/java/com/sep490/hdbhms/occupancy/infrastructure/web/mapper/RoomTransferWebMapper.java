package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.out.ContractOccupantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.domain.model.ContractOccupant;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import com.sep490.hdbhms.occupancy.domain.value_objects.TargetTransferType;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateTransferRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomTransferResponse;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class RoomTransferWebMapper {

    @Autowired
    protected LeaseContractRepository leaseContractRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected ContractOccupantRepository contractOccupantRepository;

    @Autowired
    protected TransferSettlementRepository transferSettlementRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    public CreateTransferRequestCommand toCommand(CreateTransferRequestRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateTransferRequestCommand(
                null,
                request.sourceContractId(),
                request.targetRoomId(),
                request.expectedTransferDate(),
                request.transferredTenantProfileIds(),
                request.reason()
        );
    }

    public RoomTransferResponse toResponse(RoomTransferRequest request) {
        if (request == null) {
            return null;
        }

        Room oldRoom = resolveRoom(request.getOldRoomId());
        Room targetRoom = resolveRoom(request.getTargetRoomId());
        String oldContractCode = resolveOldContractCode(request);
        Long oldRoomPrice = resolveOldRoomPrice(request);
        Long newRoomPrice = resolveNewRoomPrice(request);
        Long priceDifferenceToPay = calculatePriceDifferenceToPay(oldRoomPrice, newRoomPrice);
        Integer remainingOccupantCountAfterTransfer = resolveRemainingOccupantCountAfterTransfer(request);
        Boolean sourceRoomWillBeEmptyAfterTransfer = remainingOccupantCountAfterTransfer != null
                ? remainingOccupantCountAfterTransfer == 0
                : null;
        TransferSettlement settlement = transferSettlementRepository
            .findLatestByTransferRequestId(request.getId())
            .orElse(null);
        Long transferDifferenceInvoiceId = settlement == null ? null : settlement.getTransferDifferenceInvoiceId();
        Long oldRoomFinalInvoiceId = settlement == null ? null : settlement.getOldRoomFinalInvoiceId();
        boolean oldRoomFinalInvoicePaid = oldRoomFinalInvoiceId == null || isInvoicePaid(oldRoomFinalInvoiceId);
        Map<Long, String> transferringTenantNames = resolveTransferringTenantNames(request);
        List<Long> sourceHolderCandidateProfileIds = resolveSourceHolderCandidateProfileIds(request);
        Map<Long, String> sourceHolderCandidateNames = resolveProfileNames(sourceHolderCandidateProfileIds);

        return new RoomTransferResponse(
            request.getId(),
            request.getRequestCode(),
            request.getRequesterId(),
            request.getOldContractId(),
            oldContractCode,
            request.getOldRoomId(),
            oldRoom == null ? null : oldRoom.getRoomCode(),
            oldRoom == null ? null : oldRoom.getName(),
            request.getTargetRoomId(),
            targetRoom == null ? null : targetRoom.getRoomCode(),
            targetRoom == null ? null : targetRoom.getName(),
            request.getTransferringTenantProfileIds(),
            transferringTenantNames,
            sourceHolderCandidateProfileIds,
            sourceHolderCandidateNames,
            request.getNominatedHolderProfileId(),
            request.getTargetTransferType(),
            request.getTargetContractId(),
            request.getRequestedTransferDate(),
            request.getRequestedTransferDate(),
            request.getReason(),
            request.getReservedSlots(),
            request.getReservationExpiresAt(),
            request.getTargetHolderApprovedById(),
            request.getTargetHolderApprovedAt(),
            request.getTargetHolderRejectedAt(),
            request.getStatus(),
            request.getNewContractId(),
            request.getReplacementOldContractId(),
            oldRoomPrice,
            newRoomPrice,
            priceDifferenceToPay,
            sourceRoomWillBeEmptyAfterTransfer,
            remainingOccupantCountAfterTransfer,
            request.getCreatedAt(),
            request.getUpdatedAt(),
            request.getPositiveDifferenceSettlementType(),
            transferDifferenceInvoiceId,
            oldRoomFinalInvoiceId,
            resolvePaymentBranch(request, oldRoomPrice, newRoomPrice),
            isTransferOutHandoverRequired(request),
            isTransferInHandoverRequired(request),
            isRoomHandoverRequired(request, sourceRoomWillBeEmptyAfterTransfer),
            resolveAllowedActions(request, remainingOccupantCountAfterTransfer, priceDifferenceToPay, transferDifferenceInvoiceId, oldRoomFinalInvoicePaid),
            resolveBlockingReasons(request, remainingOccupantCountAfterTransfer, priceDifferenceToPay, transferDifferenceInvoiceId, oldRoomFinalInvoiceId, oldRoomFinalInvoicePaid)
        );
    }

    private Room resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId).orElse(null);
    }

    private boolean isInvoicePaid(Long invoiceId) {
        if (invoiceId == null) {
            return true;
        }
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM invoices WHERE invoice_id = ?",
                String.class,
                invoiceId
        );
        return !statuses.isEmpty() && "PAID".equals(statuses.get(0));
    }

    private String resolveOldContractCode(RoomTransferRequest request) {
        if (request.getOldContractId() == null) {
            return null;
        }
        return leaseContractRepository.findById(request.getOldContractId())
            .map(LeaseContract::getContractCode)
            .orElse(null);
    }

    private Long resolveOldRoomPrice(RoomTransferRequest request) {
        if (request.getOldContractId() == null) {
            return null;
        }
        return leaseContractRepository.findById(request.getOldContractId())
            .map(LeaseContract::getMonthlyRent)
            .orElse(null);
    }

    private Long resolveNewRoomPrice(RoomTransferRequest request) {
        if (request.getNewContractId() != null) {
            Long newContractRent = leaseContractRepository.findById(request.getNewContractId())
                .map(LeaseContract::getMonthlyRent)
                .orElse(null);
            if (newContractRent != null) {
                return newContractRent;
            }
        }

        if (request.getTargetRoomId() == null) {
            return null;
        }
        return roomRepository.findById(request.getTargetRoomId())
            .map(Room::getListedPrice)
            .orElse(null);
    }

    private Long calculatePriceDifferenceToPay(Long oldRoomPrice, Long newRoomPrice) {
        if (oldRoomPrice == null || newRoomPrice == null) {
            return null;
        }
        return Math.max(0, newRoomPrice - oldRoomPrice);
    }

    private Map<Long, String> resolveTransferringTenantNames(RoomTransferRequest request) {
        List<Long> profileIds = request.getTransferringTenantProfileIds();
        return resolveProfileNames(profileIds);
    }

    private Map<Long, String> resolveProfileNames(List<Long> profileIds) {
        if (profileIds == null || profileIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(profileIds.size(), "?"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT person_profile_id, full_name
                FROM person_profiles
                WHERE deleted_at IS NULL
                  AND person_profile_id IN (%s)
                """.formatted(placeholders), profileIds.toArray());

        Map<Long, String> namesById = new LinkedHashMap<>();
        for (Long profileId : profileIds) {
            namesById.put(profileId, null);
        }
        for (Map<String, Object> row : rows) {
            Object idValue = row.get("person_profile_id");
            if (!(idValue instanceof Number number)) {
                continue;
            }
            Object fullNameValue = row.get("full_name");
            String fullName = fullNameValue == null ? null : fullNameValue.toString();
            namesById.put(number.longValue(), fullName);
        }
        return namesById;
    }

    private List<Long> resolveSourceHolderCandidateProfileIds(RoomTransferRequest request) {
        if (request.getOldContractId() == null) {
            return List.of();
        }
        Set<Long> transferringIds = request.getTransferringTenantProfileIds() == null
                ? Set.of()
                : new HashSet<>(request.getTransferringTenantProfileIds());

        return contractOccupantRepository
                .findAllByContractIdAndStatus(request.getOldContractId(), OccupantStatus.ACTIVE)
                .stream()
                .map(ContractOccupant::getTenantProfileId)
                .filter(Objects::nonNull)
                .filter(profileId -> !transferringIds.contains(profileId))
                .distinct()
                .toList();
    }

    private Integer resolveRemainingOccupantCountAfterTransfer(RoomTransferRequest request) {
        if (request.getOldContractId() == null) {
            return null;
        }
        int activeOccupantCount = contractOccupantRepository
                .findAllByContractIdAndStatus(request.getOldContractId(), OccupantStatus.ACTIVE)
                .size();
        int transferringCount = request.getTransferringTenantProfileIds() == null
                ? 0
                : request.getTransferringTenantProfileIds().size();
        return Math.max(0, activeOccupantCount - transferringCount);
    }

    private String resolvePaymentBranch(RoomTransferRequest request, Long oldRoomPrice, Long newRoomPrice) {
        if (oldRoomPrice == null || newRoomPrice == null) {
            return "UNKNOWN";
        }

        long difference = newRoomPrice - oldRoomPrice;
        if (difference == 0) {
            return SettlementType.NO_DIFFERENCE.name();
        }

        SettlementType settlementType = request.getPositiveDifferenceSettlementType();
        if (settlementType == null) {
            return difference > 0 ? "UNSELECTED_POSITIVE_DIFFERENCE" : "UNSELECTED_NEGATIVE_DIFFERENCE";
        }
        if (settlementType == SettlementType.TENANT_PAY_MORE) {
            return "PAY_NOW";
        }
        return settlementType.name();
    }

    private Boolean isTransferOutHandoverRequired(RoomTransferRequest request) {
        return request.getStatus() == TransferRequestStatus.WAITING_TRANSFER_DATE
                || request.getStatus() == TransferRequestStatus.READY_FOR_HANDOVER;
    }

    private Boolean isTransferInHandoverRequired(RoomTransferRequest request) {
        return request.getTargetTransferType() == TargetTransferType.NEW_CONTRACT
                && request.getStatus() == TransferRequestStatus.WAITING_EXECUTION;
    }

    private Boolean isRoomHandoverRequired(RoomTransferRequest request, Boolean sourceRoomWillBeEmptyAfterTransfer) {
        return Boolean.TRUE.equals(sourceRoomWillBeEmptyAfterTransfer)
                && (request.getStatus() == TransferRequestStatus.WAITING_TRANSFER_DATE
                || request.getStatus() == TransferRequestStatus.READY_FOR_HANDOVER
                || request.getStatus() == TransferRequestStatus.WAITING_EXECUTION);
    }

    private List<String> resolveAllowedActions(
            RoomTransferRequest request,
            Integer remainingOccupantCountAfterTransfer,
            Long priceDifferenceToPay,
            Long transferDifferenceInvoiceId,
            boolean oldRoomFinalInvoicePaid
    ) {
        List<String> actions = new ArrayList<>();
        TransferRequestStatus status = request.getStatus();
        if (status == null || isTerminalStatus(status)) {
            return actions;
        }

        switch (status) {
            case REQUESTED, WAITING_MANAGER_APPROVAL -> {
                actions.add("CANCEL_TRANSFER_REQUEST");
            }
            case MANAGER_APPROVED, WAITING_NEW_CONTRACT -> {
                if (isSourceHolderNominationPending(request, remainingOccupantCountAfterTransfer)) {
                    actions.add("NOMINATE_SOURCE_HOLDER");
                } else {
                    actions.add("CONFIRM_TENANT_TRANSFER");
                }
            }
            case WAITING_HOLDER_RESPONSE -> {
                actions.add("ACCEPT_SOURCE_HOLDER_NOMINATION");
            }
            case WAITING_TARGET_HOLDER_APPROVAL -> {
                actions.add("APPROVE_TARGET_HOLDER");
                actions.add("REJECT_TARGET_HOLDER");
            }
            case WAITING_TENANT_CONFIRMATION -> {
                if (isImmediateDifferencePaymentPending(request, priceDifferenceToPay)) {
                    actions.add("PAY_TRANSFER_DIFFERENCE");
                } else {
                    actions.add("CONFIRM_TENANT_TRANSFER");
                }
            }
            case WAITING_PAYMENT -> {
                if (transferDifferenceInvoiceId != null) {
                    actions.add("PAY_TRANSFER_DIFFERENCE");
                }
            }
            case WAITING_CONTRACT_CONFIRMATION -> {
            }
            case WAITING_SIGNING, WAITING_CONTRACT_SIGNING -> {
                if (hasAllRequiredSignedContractFiles(request)) {
                    actions.add("SIGN_TRANSFER_CONTRACT");
                }
                actions.add("REJECT_TRANSFER_CONTRACT");
            }
            case WAITING_TRANSFER_DATE, READY_FOR_HANDOVER -> {
                actions.add("EXECUTE_TRANSFER");
            }
            case WAITING_EXECUTION -> {
                if (oldRoomFinalInvoicePaid) {
                    actions.add("COMPLETE_TRANSFER");
                } else {
                    actions.add("PAY_TRANSFER_OUT_UTILITY");
                }
            }
            default -> {
            }
        }

        return actions;
    }

    private List<String> resolveBlockingReasons(
            RoomTransferRequest request,
            Integer remainingOccupantCountAfterTransfer,
            Long priceDifferenceToPay,
            Long transferDifferenceInvoiceId,
            Long oldRoomFinalInvoiceId,
            boolean oldRoomFinalInvoicePaid
    ) {
        List<String> reasons = new ArrayList<>();
        TransferRequestStatus status = request.getStatus();
        if (status == null || isTerminalStatus(status)) {
            return reasons;
        }

        if (isSourceHolderNominationPending(request, remainingOccupantCountAfterTransfer)) {
            reasons.add("Replacement holder must be nominated before continuing.");
        }

        switch (status) {
            case REQUESTED, WAITING_MANAGER_APPROVAL -> reasons.add("Waiting for manager approval.");
            case WAITING_HOLDER_RESPONSE -> reasons.add("Waiting for nominated holder response.");
            case WAITING_TARGET_HOLDER_APPROVAL -> reasons.add("Waiting for target room holder approval.");
            case WAITING_TENANT_CONFIRMATION -> {
                if (isImmediateDifferencePaymentPending(request, priceDifferenceToPay)) {
                    reasons.add("Transfer difference invoice must be paid before request confirmation.");
                }
            }
            case WAITING_PAYMENT -> {
                if (transferDifferenceInvoiceId == null) {
                    reasons.add("Transfer difference invoice has not been created.");
                } else {
                    reasons.add("Transfer difference invoice must be paid.");
                }
            }
            case WAITING_CONTRACT_CONFIRMATION -> reasons.add("Preparing transfer contracts for manager signing.");
            case WAITING_SIGNING, WAITING_CONTRACT_SIGNING -> {
                if (hasAllRequiredSignedContractFiles(request)) {
                    reasons.add("Waiting for manager to confirm signed transfer contracts.");
                } else {
                    reasons.add("Manager must upload all signed transfer contract files before confirmation.");
                }
            }
            case WAITING_TRANSFER_DATE, READY_FOR_HANDOVER -> {
                reasons.add("Manager can start the transfer session.");
                if (Boolean.TRUE.equals(isRoomHandoverRequired(request, remainingOccupantCountAfterTransfer == null ? null : remainingOccupantCountAfterTransfer == 0))) {
                    reasons.add("Room asset handover is required because source room will be empty.");
                }
            }
            case WAITING_EXECUTION -> {
                if (oldRoomFinalInvoiceId != null && !oldRoomFinalInvoicePaid) {
                    reasons.add("Transfer utility invoice must be paid before final execution.");
                    break;
                }
                if (Boolean.TRUE.equals(isTransferInHandoverRequired(request))) {
                    reasons.add("Transfer-in baseline readings are required before final execution.");
                } else {
                    reasons.add("Waiting for final execution.");
                }
            }
            default -> {
            }
        }

        return reasons;
    }

    private boolean isImmediateDifferencePaymentPending(RoomTransferRequest request, Long priceDifferenceToPay) {
        return priceDifferenceToPay != null
                && priceDifferenceToPay > 0
                && request.getPositiveDifferenceSettlementType() == SettlementType.TENANT_PAY_MORE;
    }

    private boolean hasAllRequiredSignedContractFiles(RoomTransferRequest request) {
        boolean hasRequiredContract = false;
        if (request.getNewContractId() != null) {
            hasRequiredContract = true;
            boolean uploaded = leaseContractRepository.findById(request.getNewContractId())
                    .map(contract -> contract.getContractFileId() != null)
                    .orElse(false);
            if (!uploaded) {
                return false;
            }
        }
        if (request.getReplacementOldContractId() != null) {
            hasRequiredContract = true;
            boolean uploaded = leaseContractRepository.findById(request.getReplacementOldContractId())
                    .map(contract -> contract.getContractFileId() != null)
                    .orElse(false);
            if (!uploaded) {
                return false;
            }
        }
        return hasRequiredContract;
    }

    private boolean isSourceHolderNominationPending(RoomTransferRequest request, Integer remainingOccupantCountAfterTransfer) {
        if (remainingOccupantCountAfterTransfer == null || remainingOccupantCountAfterTransfer <= 0) {
            return false;
        }
        if (request.getNominatedHolderProfileId() != null) {
            return false;
        }
        return leaseContractRepository.findById(request.getOldContractId())
                .map(LeaseContract::getPrimaryTenantProfileId)
                .map(primaryTenantProfileId -> request.getTransferringTenantProfileIds() != null
                        && request.getTransferringTenantProfileIds().contains(primaryTenantProfileId))
                .orElse(false);
    }

    private boolean isTerminalStatus(TransferRequestStatus status) {
        return status == TransferRequestStatus.CANCELLED
                || status == TransferRequestStatus.REJECTED
                || status == TransferRequestStatus.EXPIRED
                || status == TransferRequestStatus.EXECUTED
                || status == TransferRequestStatus.COMPLETED;
    }
}
