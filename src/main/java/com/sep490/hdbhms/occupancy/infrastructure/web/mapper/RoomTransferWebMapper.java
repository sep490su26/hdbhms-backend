package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.out.ContractOccupantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.DepositTransferRecordRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TransferSettlementRepository;
import com.sep490.hdbhms.occupancy.domain.model.ContractOccupant;
import com.sep490.hdbhms.occupancy.domain.model.DepositTransferRecord;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class RoomTransferWebMapper {
    private static final String SOURCE_ROOM_TRANSFER_COMPENSATION = "ROOM_TRANSFER_COMPENSATION";

    @Autowired
    protected LeaseContractRepository leaseContractRepository;

    @Autowired
    protected RoomRepository roomRepository;

    @Autowired
    protected ContractOccupantRepository contractOccupantRepository;

    @Autowired
    protected TransferSettlementRepository transferSettlementRepository;

    @Autowired
    protected DepositTransferRecordRepository depositTransferRecordRepository;

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
        Long priceDifferenceToPay = calculatePriceDifferenceToPay(request, oldRoomPrice, newRoomPrice);
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
        List<Long> oldRoomCompensationInvoiceIds = resolveOldRoomCompensationInvoiceIds(request);
        List<Long> unpaidOldRoomCompensationInvoiceIds = oldRoomCompensationInvoiceIds.stream()
                .filter(invoiceId -> !isInvoicePaid(invoiceId))
                .toList();
        boolean oldRoomCheckoutInvoicesPaid = oldRoomFinalInvoicePaid && unpaidOldRoomCompensationInvoiceIds.isEmpty();
        Map<Long, String> transferringTenantNames = resolveTransferringTenantNames(request);
        TenantContact requesterContact = resolveRequesterContact(request, transferringTenantNames);
        List<Long> sourceHolderCandidateProfileIds = resolveSourceHolderCandidateProfileIds(request);
        Map<Long, String> sourceHolderCandidateNames = resolveProfileNames(sourceHolderCandidateProfileIds);
        RoomTransferResponse.DebtSummary debtSummary = resolveDebtSummary(request, oldRoomPrice);
        RoomTransferResponse.ViolationSummary violationSummary = resolveViolationSummary(request);
        Integer transferCountThisYear = resolveTransferCountThisYear(request);
        List<String> eligibilityWarnings = resolveEligibilityWarnings(debtSummary, violationSummary, transferCountThisYear);

        return new RoomTransferResponse(
            request.getId(),
            request.getRequestCode(),
            request.getRequesterId(),
            request.getOldContractId(),
            oldContractCode,
            request.getOldRoomId(),
            oldRoom == null ? null : oldRoom.getRoomCode(),
            oldRoom == null ? null : oldRoom.getName(),
            oldRoom == null ? null : oldRoom.getFloorId(),
            request.getTargetRoomId(),
            targetRoom == null ? null : targetRoom.getRoomCode(),
            targetRoom == null ? null : targetRoom.getName(),
            targetRoom == null ? null : targetRoom.getFloorId(),
            requesterContact.name(),
            requesterContact.phone(),
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
            request.getApprovedById(),
            resolveUserDisplayName(request.getApprovedById()),
            request.getApprovedAt(),
            request.getExecutedAt(),
            request.getCompletedAt(),
            resolveActualTransferDate(request),
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
            resolveDepositTransferSummary(request),
            debtSummary,
            violationSummary,
            transferCountThisYear,
            request.getEligibilityCheckedAt(),
            request.getEligibleAtCreation(),
            request.getEligibilitySnapshot(),
            request.getViolationSnapshot(),
            request.getTransferHistorySnapshot(),
            eligibilityWarnings,
            resolvePaymentBranch(request, oldRoomPrice, newRoomPrice),
            isTransferOutHandoverRequired(request),
            isTransferInHandoverRequired(request),
            isRoomHandoverRequired(request, sourceRoomWillBeEmptyAfterTransfer),
            resolveAllowedActions(request, remainingOccupantCountAfterTransfer, priceDifferenceToPay, transferDifferenceInvoiceId, oldRoomCheckoutInvoicesPaid),
            resolveBlockingReasons(request, remainingOccupantCountAfterTransfer, priceDifferenceToPay, transferDifferenceInvoiceId, oldRoomFinalInvoiceId, oldRoomFinalInvoicePaid, unpaidOldRoomCompensationInvoiceIds)
        );
    }

    private RoomTransferResponse.DepositTransferSummary resolveDepositTransferSummary(RoomTransferRequest request) {
        if (request == null || request.getId() == null) {
            return null;
        }
        DepositTransferRecord record = depositTransferRecordRepository
                .findByTransferRequestId(request.getId())
                .orElse(null);
        if (record == null) {
            return null;
        }
        return new RoomTransferResponse.DepositTransferSummary(
                record.getId(),
                record.getOldContractId(),
                record.getNewContractId(),
                record.getOldDepositAgreementId(),
                record.getAmount(),
                record.getFromRoomId(),
                record.getToRoomId(),
                record.getStatus(),
                record.getEffectiveDate(),
                record.getNote()
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

    private List<Long> resolveOldRoomCompensationInvoiceIds(RoomTransferRequest request) {
        if (request == null || request.getId() == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                        SELECT DISTINCT invoice.invoice_id
                        FROM invoices invoice
                        JOIN invoice_lines line ON line.invoice_id = invoice.invoice_id
                        WHERE line.source_type = ?
                          AND line.source_id = ?
                          AND line.line_type = 'MANUAL_ADJUSTMENT'
                          AND invoice.status <> 'VOIDED'
                        ORDER BY invoice.invoice_id DESC
                        """,
                (rs, rowNum) -> rs.getLong("invoice_id"),
                SOURCE_ROOM_TRANSFER_COMPENSATION,
                request.getId()
        );
    }

    private RoomTransferResponse.DebtSummary resolveDebtSummary(RoomTransferRequest request, Long debtLimitAmount) {
        if (request.getOldContractId() == null) {
            return emptyDebtSummary(debtLimitAmount);
        }
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
                """, request.getOldContractId());

        long rentDebt = asLong(row.get("rent_debt_amount"));
        long utilityDebt = asLong(row.get("utility_debt_amount"));
        long otherDebt = asLong(row.get("other_debt_amount"));
        long totalDebt = rentDebt + utilityDebt + otherDebt;
        return new RoomTransferResponse.DebtSummary(
                rentDebt,
                utilityDebt,
                otherDebt,
                asInteger(row.get("rent_debt_months")),
                asInteger(row.get("utility_debt_months")),
                totalDebt,
                debtLimitAmount,
                debtLimitAmount != null && totalDebt > debtLimitAmount
        );
    }

    private RoomTransferResponse.DebtSummary emptyDebtSummary(Long debtLimitAmount) {
        return new RoomTransferResponse.DebtSummary(
                0L,
                0L,
                0L,
                0,
                0,
                0L,
                debtLimitAmount,
                false
        );
    }

    private RoomTransferResponse.ViolationSummary resolveViolationSummary(RoomTransferRequest request) {
        if (request.getOldContractId() == null) {
            return new RoomTransferResponse.ViolationSummary(0, List.of());
        }
        Integer totalCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rule_violations
                WHERE contract_id = ?
                  AND status IN ('RECORDED', 'INVOICED')
                """, Integer.class, request.getOldContractId());
        List<String> latestDescriptions = jdbcTemplate.queryForList("""
                SELECT description
                FROM rule_violations
                WHERE contract_id = ?
                  AND status IN ('RECORDED', 'INVOICED')
                ORDER BY violation_date DESC, rule_violation_id DESC
                LIMIT 5
                """, String.class, request.getOldContractId());
        return new RoomTransferResponse.ViolationSummary(
                totalCount == null ? 0 : totalCount,
                latestDescriptions
        );
    }

    private Integer resolveTransferCountThisYear(RoomTransferRequest request) {
        if (request.getRequesterId() == null) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM room_transfer_requests
                WHERE requester_id = ?
                  AND status IN ('EXECUTED', 'COMPLETED')
                  AND created_at >= MAKEDATE(YEAR(CURRENT_DATE), 1)
                """, Integer.class, request.getRequesterId());
        return count == null ? 0 : count;
    }

    private List<String> resolveEligibilityWarnings(
            RoomTransferResponse.DebtSummary debtSummary,
            RoomTransferResponse.ViolationSummary violationSummary,
            Integer transferCountThisYear
    ) {
        List<String> warnings = new ArrayList<>();
        if (debtSummary != null && Boolean.TRUE.equals(debtSummary.overLimit())) {
            warnings.add("Debt is over the configured warning threshold.");
        }
        if (violationSummary != null && violationSummary.totalCount() != null && violationSummary.totalCount() > 0) {
            warnings.add("Tenant has recorded rule violations.");
        }
        if (transferCountThisYear != null && transferCountThisYear > 0) {
            warnings.add("Tenant already has room transfer history this year.");
        }
        return warnings;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private int asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
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

    private Long calculatePriceDifferenceToPay(RoomTransferRequest request, Long oldRoomPrice, Long newRoomPrice) {
        if (oldRoomPrice == null || newRoomPrice == null) {
            return null;
        }
        int chargeableMonths = resolveChargeableRemainingMonths(request);
        return Math.max(0, (newRoomPrice - oldRoomPrice) * chargeableMonths);
    }

    private int resolveChargeableRemainingMonths(RoomTransferRequest request) {
        if (request.getOldContractId() == null) {
            return 0;
        }
        LeaseContract oldContract = leaseContractRepository.findById(request.getOldContractId()).orElse(null);
        if (oldContract == null) {
            return 0;
        }
        int paymentCycleMonths = oldContract.getPaymentCycleMonths() == null
                ? 1
                : Math.max(1, oldContract.getPaymentCycleMonths());
        if (paymentCycleMonths <= 1) {
            return 0;
        }
        java.time.LocalDate transferDate = request.getRequestedTransferDate() == null
                ? java.time.LocalDate.now()
                : request.getRequestedTransferDate();
        java.time.LocalDate cycleStart = java.util.Optional.ofNullable(oldContract.getRentStartDate())
                .or(() -> java.util.Optional.ofNullable(oldContract.getStartDate()))
                .orElse(transferDate.withDayOfMonth(1));
        while (!transferDate.isBefore(cycleStart.plusMonths(paymentCycleMonths))) {
            cycleStart = cycleStart.plusMonths(paymentCycleMonths);
        }
        while (transferDate.isBefore(cycleStart)) {
            cycleStart = cycleStart.minusMonths(paymentCycleMonths);
        }
        java.time.LocalDate cycleEndExclusive = cycleStart.plusMonths(paymentCycleMonths);
        java.time.LocalDate chargeStart = transferDate.getDayOfMonth() >= 11
                ? transferDate.plusMonths(1).withDayOfMonth(1)
                : transferDate.withDayOfMonth(1);
        java.time.LocalDate cycleStartMonth = cycleStart.withDayOfMonth(1);
        if (chargeStart.isBefore(cycleStartMonth)) {
            chargeStart = cycleStartMonth;
        }
        if (!chargeStart.isBefore(cycleEndExclusive)) {
            return 0;
        }
        return Math.max(
                0,
                (int) java.time.temporal.ChronoUnit.MONTHS.between(
                        java.time.YearMonth.from(chargeStart),
                        java.time.YearMonth.from(cycleEndExclusive)
                )
        );
    }

    private Map<Long, String> resolveTransferringTenantNames(RoomTransferRequest request) {
        List<Long> profileIds = request.getTransferringTenantProfileIds();
        return resolveProfileNames(profileIds);
    }

    private TenantContact resolveRequesterContact(RoomTransferRequest request, Map<Long, String> fallbackNames) {
        if (request == null || request.getRequesterId() == null) {
            return new TenantContact(firstTransferredTenantName(fallbackNames), null);
        }

        List<TenantContact> contacts = jdbcTemplate.query("""
                        SELECT
                            COALESCE(NULLIF(pp.full_name, ''), u.email, u.phone) AS display_name,
                            COALESCE(NULLIF(pp.phone, ''), u.phone) AS phone
                        FROM tenants t
                        JOIN users u ON u.user_id = t.user_id
                        LEFT JOIN person_profiles pp ON pp.user_id = u.user_id AND pp.deleted_at IS NULL
                        WHERE t.tenant_id = ?
                        ORDER BY pp.person_profile_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new TenantContact(rs.getString("display_name"), rs.getString("phone")),
                request.getRequesterId()
        );

        if (contacts.isEmpty()) {
            return new TenantContact(firstTransferredTenantName(fallbackNames), null);
        }
        TenantContact contact = contacts.get(0);
        return new TenantContact(firstNonBlank(contact.name(), firstTransferredTenantName(fallbackNames)), contact.phone());
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                        SELECT COALESCE(NULLIF(pp.full_name, ''), u.email, u.phone) AS display_name
                        FROM users u
                        LEFT JOIN person_profiles pp ON pp.user_id = u.user_id AND pp.deleted_at IS NULL
                        WHERE u.user_id = ?
                        ORDER BY pp.person_profile_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString("display_name"),
                userId
        ).stream().findFirst().orElse(null);
    }

    private LocalDateTime resolveActualTransferDate(RoomTransferRequest request) {
        if (request.getCompletedAt() != null) {
            return request.getCompletedAt();
        }
        if (request.getExecutedAt() != null) {
            return request.getExecutedAt();
        }
        return request.getRequestedTransferDate() == null ? null : request.getRequestedTransferDate().atStartOfDay();
    }

    private String firstTransferredTenantName(Map<Long, String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return names.values().stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
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

    private record TenantContact(String name, String phone) {}

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
            boolean oldRoomFinalInvoicePaid,
            List<Long> unpaidOldRoomCompensationInvoiceIds
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
                boolean blockedByOldRoomInvoices = false;
                if (oldRoomFinalInvoiceId != null && !oldRoomFinalInvoicePaid) {
                    reasons.add("Hóa đơn điện nước chốt phòng cũ phải được thanh toán trước khi hoàn tất chuyển phòng.");
                    blockedByOldRoomInvoices = true;
                }
                if (unpaidOldRoomCompensationInvoiceIds != null && !unpaidOldRoomCompensationInvoiceIds.isEmpty()) {
                    reasons.add("Hóa đơn bồi thường phòng cũ #"
                            + unpaidOldRoomCompensationInvoiceIds.get(0)
                            + " phải được thanh toán trước khi hoàn tất chuyển phòng.");
                    blockedByOldRoomInvoices = true;
                }
                if (blockedByOldRoomInvoices) {
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
