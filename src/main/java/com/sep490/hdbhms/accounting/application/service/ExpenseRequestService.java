package com.sep490.hdbhms.accounting.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseAttachmentType;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpensePaymentMethod;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseStatus;
import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpenseApprovalRequestEntity;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpenseAttachmentEntity;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpensePaymentEntity;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.OperatingExpenseEntity;
import com.sep490.hdbhms.accounting.infrastructure.persistence.jpa.JpaExpenseApprovalRequestRepository;
import com.sep490.hdbhms.accounting.infrastructure.persistence.jpa.JpaExpenseAttachmentRepository;
import com.sep490.hdbhms.accounting.infrastructure.persistence.jpa.JpaExpensePaymentRepository;
import com.sep490.hdbhms.accounting.infrastructure.persistence.jpa.JpaOperatingExpenseRepository;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.CreateExpenseAttachmentRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.CreateExpenseRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.MarkExpensePaidRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.request.RejectExpenseRequest;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.response.ExpenseAttachmentResponse;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.response.ExpensePaymentResponse;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.response.ExpenseRequestResponse;
import com.sep490.hdbhms.accounting.infrastructure.web.dto.response.ExpenseTimelineResponse;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.InvoiceEntity;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa.JpaInvoiceRepository;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEventEntity;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestEventRepository;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.jpa.JpaChangeRequestRepository;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.notification.application.service.BusinessNotificationPublisher;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.id.SnowflakeIdGenerator;
import com.sep490.hdbhms.shared.utils.RequestCodeBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseRequestService {
    private static final String SOURCE_REQUEST_TYPE_CONTRACT_LIQUIDATION = "CONTRACT_LIQUIDATION";
    private static final List<RequestStatus> LIQUIDATION_REQUEST_STATUSES = List.of(
            RequestStatus.PENDING,
            RequestStatus.PROCESSING,
            RequestStatus.APPROVED,
            RequestStatus.COMPLETED
    );

    JpaOperatingExpenseRepository operatingExpenseRepository;
    JpaExpenseApprovalRequestRepository approvalRequestRepository;
    JpaExpenseAttachmentRepository attachmentRepository;
    JpaExpensePaymentRepository paymentRepository;
    JpaChangeRequestRepository changeRequestRepository;
    JpaChangeRequestEventRepository changeRequestEventRepository;
    JpaPropertyRepository propertyRepository;
    JpaRoomRepository roomRepository;
    JpaUserRepository userRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaInvoiceRepository invoiceRepository;
    BusinessNotificationPublisher notificationPublisher;
    SnowflakeIdGenerator snowflakeIdGenerator;
    ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<ExpenseRequestResponse> listRequests(
            Long propertyId,
            Long roomId,
            ExpenseType expenseType,
            ExpenseStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword,
            Pageable pageable
    ) {
        Page<ExpenseRequestResponse> page = operatingExpenseRepository.findFiltered(
                        propertyId,
                        roomId,
                        expenseType,
                        status,
                        fromDate,
                        toDate,
                        normalizeKeyword(keyword),
                        pageable
                )
                .map(this::toResponse);
        return PageResponse.fromPageToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public ExpenseRequestResponse getRequest(Long id) {
        return toResponse(requireExpense(id));
    }

    @Transactional
    public ExpenseRequestResponse createRequest(CreateExpenseRequest request, Long currentUserId, Role currentRole) {
        if (currentRole != Role.MANAGER && currentRole != Role.OWNER) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
        if (request == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (request.propertyId() == null || request.expenseType() == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        String reason = blankToNull(request.reason());
        if (reason == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        long amount = requirePositiveAmount(request.amount());
        UserEntity requester = requireUser(currentUserId);
        PropertyEntity property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        RoomEntity room = resolveRoom(request.roomId(), property.getId());

        OperatingExpenseEntity expense = operatingExpenseRepository.save(OperatingExpenseEntity.builder()
                .property(property)
                .room(room)
                .expenseCode("EXP-" + snowflakeIdGenerator.next())
                .expenseType(request.expenseType())
                .description(defaultText(request.description(), reason))
                .amount(amount)
                .expenseDate(request.expectedPaymentDate() == null ? LocalDate.now() : request.expectedPaymentDate())
                .status(ExpenseStatus.PENDING_APPROVAL)
                .createdBy(requester)
                .build());

        ChangeRequestEntity changeRequest = changeRequestRepository.save(ChangeRequestEntity.builder()
                .requestCode(nextRequestCode(RequestType.EXPENSE_APPROVAL, room == null ? null : room.getRoomCode()))
                .requestType(RequestType.EXPENSE_APPROVAL)
                .requester(requester)
                .requesterRole(toRequesterRole(currentRole))
                .targetType(TargetType.OPERATING_EXPENSE)
                .targetId(expense.getId())
                .title("Yêu cầu chi " + expense.getExpenseCode())
                .description(reason)
                .requestPayload(toJson(payload(
                        "operatingExpenseId", expense.getId(),
                        "expenseCode", expense.getExpenseCode(),
                        "amount", expense.getAmount(),
                        "propertyId", property.getId(),
                        "roomId", room == null ? null : room.getId()
                )))
                .assignedRole(AssignedRole.OWNER)
                .status(RequestStatus.PENDING)
                .build());

        approvalRequestRepository.save(ExpenseApprovalRequestEntity.builder()
                .operatingExpense(expense)
                .changeRequest(changeRequest)
                .reason(reason)
                .vendorName(blankToNull(request.vendorName()))
                .expectedPaymentDate(request.expectedPaymentDate())
                .build());
        saveAttachments(expense, request.attachments());
        recordChangeEvent(changeRequest, null, RequestStatus.PENDING, "Tạo yêu cầu chi", currentUserId);
        notifyOwners(expense, changeRequest);
        return toResponse(expense);
    }

    @Transactional
    public ExpenseRequestResponse approveRequest(Long id, Long ownerId) {
        OperatingExpenseEntity expense = requireExpense(id);
        ExpenseApprovalRequestEntity approval = requireApproval(expense.getId());
        ChangeRequestEntity changeRequest = approval.getChangeRequest();
        requirePending(expense, changeRequest);

        LocalDateTime now = LocalDateTime.now();
        UserEntity owner = requireUser(ownerId);
        changeRequest.setStatus(RequestStatus.APPROVED);
        changeRequest.setResolvedBy(owner);
        changeRequest.setResolvedAt(now);
        operatingExpenseRepository.save(updateExpenseStatus(expense, ExpenseStatus.READY_FOR_PAYMENT, owner, now));
        changeRequestRepository.save(changeRequest);
        recordChangeEvent(changeRequest, RequestStatus.PENDING, RequestStatus.APPROVED,
                "Chủ trọ đã duyệt yêu cầu chi, đang chờ thanh toán", ownerId);
        notifyRequester(expense, changeRequest, "EXPENSE_APPROVED");
        syncLinkedLiquidationRefund(expense, changeRequest);
        return toResponse(expense);
    }

    @Transactional
    public ExpenseRequestResponse rejectRequest(Long id, RejectExpenseRequest request, Long ownerId) {
        String reason = blankToNull(request == null ? null : request.reason());
        if (reason == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        OperatingExpenseEntity expense = requireExpense(id);
        ExpenseApprovalRequestEntity approval = requireApproval(expense.getId());
        ChangeRequestEntity changeRequest = approval.getChangeRequest();
        requirePending(expense, changeRequest);

        LocalDateTime now = LocalDateTime.now();
        UserEntity owner = requireUser(ownerId);
        changeRequest.setStatus(RequestStatus.REJECTED);
        changeRequest.setResolutionNote(reason);
        changeRequest.setResolvedBy(owner);
        changeRequest.setResolvedAt(now);
        expense.setStatus(ExpenseStatus.REJECTED);
        operatingExpenseRepository.save(expense);
        changeRequestRepository.save(changeRequest);
        recordChangeEvent(changeRequest, RequestStatus.PENDING, RequestStatus.REJECTED, reason, ownerId);
        notifyRequester(expense, changeRequest, "EXPENSE_REJECTED");
        syncLinkedLiquidationRefund(expense, changeRequest);
        return toResponse(expense);
    }

    @Transactional
    public ExpenseRequestResponse cancelRequest(Long id, Long currentUserId, Role currentRole) {
        OperatingExpenseEntity expense = requireExpense(id);
        ExpenseApprovalRequestEntity approval = requireApproval(expense.getId());
        ChangeRequestEntity changeRequest = approval.getChangeRequest();
        requirePending(expense, changeRequest);
        if (currentRole != Role.OWNER && !Objects.equals(changeRequest.getRequester().getId(), currentUserId)) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }

        changeRequest.setStatus(RequestStatus.CANCELLED);
        changeRequest.setResolvedBy(requireUser(currentUserId));
        changeRequest.setResolvedAt(LocalDateTime.now());
        expense.setStatus(ExpenseStatus.CANCELLED);
        operatingExpenseRepository.save(expense);
        changeRequestRepository.save(changeRequest);
        recordChangeEvent(changeRequest, RequestStatus.PENDING, RequestStatus.CANCELLED,
                "Hủy yêu cầu chi", currentUserId);
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public LiquidationDepositRefundLink getLiquidationDepositRefundLink(Long contractId) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        if (sourceRequest == null) {
            return LiquidationDepositRefundLink.empty();
        }
        return toLiquidationDepositRefundLink(sourceRequest, payloadMap(sourceRequest.getRequestPayload()));
    }

    @Transactional(readOnly = true)
    public LiquidationDepositForfeitureLink getLiquidationDepositForfeitureLink(Long contractId) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        if (sourceRequest == null) {
            return LiquidationDepositForfeitureLink.empty();
        }
        return toLiquidationDepositForfeitureLink(sourceRequest, payloadMap(sourceRequest.getRequestPayload()));
    }

    @Transactional
    public LiquidationDepositRefundLink ensureLiquidationDepositRefundRequest(
            Long contractId,
            String contractCode,
            Long propertyId,
            Long roomId,
            String roomCode,
            Long amount,
            LocalDate liquidationDate,
            Long currentUserId
    ) {
        return ensureLiquidationDepositRefundRequest(
                contractId,
                contractCode,
                propertyId,
                roomId,
                roomCode,
                amount,
                liquidationDate,
                currentUserId,
                null
        );
    }

    @Transactional
    public LiquidationDepositRefundLink ensureLiquidationDepositRefundRequest(
            Long contractId,
            String contractCode,
            Long propertyId,
            Long roomId,
            String roomCode,
            Long amount,
            LocalDate liquidationDate,
            Long currentUserId,
            Long tenantUserId
    ) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        long refundAmount = safeAmount(amount);
        if (refundAmount <= 0) {
            if (sourceRequest == null) {
                return LiquidationDepositRefundLink.empty();
            }
            Map<String, Object> sourcePayload = payloadMap(sourceRequest.getRequestPayload());
            markRefundNotRequired(sourceRequest, sourcePayload);
            return toLiquidationDepositRefundLink(sourceRequest, sourcePayload);
        }

        if (sourceRequest == null) {
            UserEntity requester = requireUser(currentUserId);
            PropertyEntity property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cơ sở."));
            RoomEntity room = resolveRoom(roomId, property.getId());
            String roomLabel = roomCode == null || roomCode.isBlank() ? "" : " phòng " + roomCode;
            String reason = "Hoàn cọc thanh lý hợp đồng " + defaultText(contractCode, "#" + contractId);
            sourceRequest = changeRequestRepository.save(ChangeRequestEntity.builder()
                    .requestCode(nextRequestCode(
                            RequestType.CONTRACT_LIQUIDATION,
                            room == null ? roomCode : room.getRoomCode()
                    ))
                    .requestType(RequestType.CONTRACT_LIQUIDATION)
                    .requester(requester)
                    .requesterRole(toRequesterRole(requester.getRole()))
                    .targetType(TargetType.CONTRACT)
                    .targetId(contractId)
                    .title("Yêu cầu thanh lý hợp đồng " + defaultText(contractCode, "#" + contractId))
                    .description(reason + roomLabel)
                    .requestPayload(toJson(payload(
                            "requestKind", SOURCE_REQUEST_TYPE_CONTRACT_LIQUIDATION,
                            "contractId", contractId,
                            "contractCode", contractCode,
                            "roomId", room == null ? null : room.getId(),
                            "roomCode", roomCode,
                            "primaryTenantUserId", tenantUserId,
                            "liquidationDate", liquidationDate,
                            "liquidationStage", "WAITING_DEPOSIT_REFUND",
                            "depositRefundStatus", "PENDING",
                            "depositRefundAmount", refundAmount
                    )))
                    .assignedRole(AssignedRole.OWNER)
                    .status(RequestStatus.PENDING)
                    .build());
            sourceRequest.setStatus(RequestStatus.PROCESSING);
            sourceRequest = changeRequestRepository.save(sourceRequest);
            recordChangeEvent(sourceRequest, null, RequestStatus.PROCESSING,
                    "Tạo yêu cầu thanh lý để theo dõi hoàn cọc", currentUserId);
        }

        Map<String, Object> sourcePayload = payloadMap(sourceRequest.getRequestPayload());

        Long existingExpenseId = toLong(sourcePayload.get("depositRefundExpenseId"));
        OperatingExpenseEntity existingExpense = existingExpenseId == null
                ? null
                : operatingExpenseRepository.findById(existingExpenseId).orElse(null);
        if (existingExpense != null) {
            updateLinkedRefundExpense(existingExpense, refundAmount);
            ExpenseApprovalRequestEntity approval = approvalRequestRepository
                    .findByOperatingExpense_Id(existingExpense.getId())
                    .orElse(null);
            syncLiquidationRefundPayload(sourceRequest, existingExpense, approval, null, null, null);
            return toLiquidationDepositRefundLink(sourceRequest, payloadMap(sourceRequest.getRequestPayload()));
        }

        UserEntity requester = requireUser(currentUserId);
        PropertyEntity property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        RoomEntity room = resolveRoom(roomId, property.getId());
        String reason = "Hoàn cọc thanh lý hợp đồng " + defaultText(contractCode, "#" + contractId);
        String roomLabel = roomCode == null || roomCode.isBlank() ? "" : " phòng " + roomCode;

        OperatingExpenseEntity expense = operatingExpenseRepository.save(OperatingExpenseEntity.builder()
                .property(property)
                .room(room)
                .expenseCode("EXP-" + snowflakeIdGenerator.next())
                .expenseType(ExpenseType.OTHER)
                .description(reason + roomLabel)
                .amount(refundAmount)
                .expenseDate(liquidationDate == null ? LocalDate.now() : liquidationDate)
                .status(ExpenseStatus.PENDING_APPROVAL)
                .createdBy(requester)
                .build());

        ChangeRequestEntity expenseChangeRequest = changeRequestRepository.save(ChangeRequestEntity.builder()
                .requestCode(nextRequestCode(
                        RequestType.EXPENSE_APPROVAL,
                        room == null ? roomCode : room.getRoomCode()
                ))
                .requestType(RequestType.EXPENSE_APPROVAL)
                .requester(requester)
                .requesterRole(toRequesterRole(requester.getRole()))
                .targetType(TargetType.OPERATING_EXPENSE)
                .targetId(expense.getId())
                .title("Yêu cầu duyệt hoàn cọc " + defaultText(contractCode, "#" + contractId))
                .description(reason + roomLabel)
                .requestPayload(toJson(payload(
                        "operatingExpenseId", expense.getId(),
                        "expenseCode", expense.getExpenseCode(),
                        "amount", expense.getAmount(),
                        "propertyId", property.getId(),
                        "roomId", room == null ? null : room.getId(),
                        "roomCode", roomCode,
                        "sourceRequestType", SOURCE_REQUEST_TYPE_CONTRACT_LIQUIDATION,
                        "liquidationChangeRequestId", sourceRequest.getId(),
                        "contractId", contractId,
                        "contractCode", contractCode,
                        "depositRefundAmount", refundAmount,
                        "liquidationDate", liquidationDate
                )))
                .assignedRole(AssignedRole.OWNER)
                .status(RequestStatus.PENDING)
                .build());

        ExpenseApprovalRequestEntity approval = approvalRequestRepository.save(ExpenseApprovalRequestEntity.builder()
                .operatingExpense(expense)
                .changeRequest(expenseChangeRequest)
                .reason(reason + roomLabel)
                .expectedPaymentDate(liquidationDate)
                .build());
        recordChangeEvent(expenseChangeRequest, null, RequestStatus.PENDING, "Tạo yêu cầu duyệt hoàn cọc", currentUserId);
        notifyOwners(expense, expenseChangeRequest);
        syncLiquidationRefundPayload(sourceRequest, expense, approval, null, null, null);
        return toLiquidationDepositRefundLink(sourceRequest, payloadMap(sourceRequest.getRequestPayload()));
    }

    @Transactional
    public LiquidationDepositForfeitureLink ensureLiquidationDepositForfeitureRequest(
            Long contractId,
            String contractCode,
            Long propertyId,
            Long roomId,
            String roomCode,
            Long amount,
            String reason,
            LocalDate liquidationDate,
            Long currentUserId,
            Long tenantUserId
    ) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        long forfeitureAmount = safeAmount(amount);
        String forfeitureReason = reason == null || reason.isBlank() ? null : reason.trim();
        boolean sourceCreated = false;

        if (sourceRequest == null && forfeitureAmount > 0) {
            UserEntity requester = requireUser(currentUserId);
            PropertyEntity property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
            RoomEntity room = resolveRoom(roomId, property.getId());
            String roomLabel = roomCode == null || roomCode.isBlank() ? "" : " phong " + roomCode;
            sourceRequest = changeRequestRepository.save(ChangeRequestEntity.builder()
                    .requestCode(nextRequestCode(
                            RequestType.CONTRACT_LIQUIDATION,
                            room == null ? roomCode : room.getRoomCode()
                    ))
                    .requestType(RequestType.CONTRACT_LIQUIDATION)
                    .requester(requester)
                    .requesterRole(toRequesterRole(requester.getRole()))
                    .targetType(TargetType.CONTRACT)
                    .targetId(contractId)
                    .title("Yeu cau xac nhan mat coc hop dong " + defaultText(contractCode, "#" + contractId))
                    .description("Khach thue can xac nhan khoan mat coc" + roomLabel)
                    .requestPayload(toJson(payload(
                            "requestKind", SOURCE_REQUEST_TYPE_CONTRACT_LIQUIDATION,
                            "contractId", contractId,
                            "contractCode", contractCode,
                            "roomId", room == null ? null : room.getId(),
                            "roomCode", roomCode,
                            "primaryTenantUserId", tenantUserId,
                            "liquidationDate", liquidationDate,
                            "liquidationStage", "WAITING_DEPOSIT_FORFEITURE_CONFIRMATION",
                            "depositForfeitureStatus", "PENDING_TENANT_CONFIRMATION",
                            "depositForfeitureAmount", forfeitureAmount,
                            "depositForfeitureReason", forfeitureReason,
                            "liquidationChecklist", payload(
                                    "depositForfeitureConfirmed", false
                            )
                    )))
                    .assignedRole(AssignedRole.OWNER)
                    .status(RequestStatus.PROCESSING)
                    .build());
            sourceCreated = true;
            recordChangeEvent(sourceRequest, null, RequestStatus.PROCESSING,
                    "Tao yeu cau xac nhan mat coc", currentUserId);
        }

        if (sourceRequest == null) {
            return LiquidationDepositForfeitureLink.empty();
        }

        Map<String, Object> sourcePayload = payloadMap(sourceRequest.getRequestPayload());
        String currentStatus = Objects.toString(sourcePayload.get("depositForfeitureStatus"), "NOT_REQUIRED");
        long currentAmount = safeAmount(toLong(sourcePayload.get("depositForfeitureAmount")));
        String currentReason = sourcePayload.get("depositForfeitureReason") == null
                ? null
                : sourcePayload.get("depositForfeitureReason").toString();
        String currentLiquidationDate = sourcePayload.get("liquidationDate") == null
                ? null
                : sourcePayload.get("liquidationDate").toString();
        String requestedLiquidationDate = liquidationDate == null ? null : liquidationDate.toString();
        boolean sameDecision = currentAmount == forfeitureAmount
                && Objects.equals(blankToNull(currentReason), blankToNull(forfeitureReason))
                && Objects.equals(currentLiquidationDate, requestedLiquidationDate);

        sourcePayload.put("depositForfeitureAmount", forfeitureAmount);
        sourcePayload.put("depositForfeitureReason", forfeitureReason);
        sourcePayload.put("liquidationDate", liquidationDate);
        boolean notifyTenant = sourceCreated;
        if (forfeitureAmount <= 0) {
            boolean wasBlocking = "PENDING_TENANT_CONFIRMATION".equals(currentStatus)
                    || "DISPUTED".equals(currentStatus);
            sourcePayload.put("depositForfeitureStatus", "NOT_REQUIRED");
            markChecklist(sourcePayload, "depositForfeitureConfirmed", true);
            if (wasBlocking) {
                sourcePayload.put("liquidationStage", liquidationStageAfterSettlementSync(
                        sourcePayload,
                        syncFinalInvoicePaid(sourcePayload),
                        Objects.toString(sourcePayload.get("depositRefundStatus"), "")
                ));
            }
        } else if (!sameDecision || (!"TENANT_CONFIRMED".equals(currentStatus)
                && !"PENDING_TENANT_CONFIRMATION".equals(currentStatus)
                && !"DISPUTED".equals(currentStatus))) {
            sourcePayload.put("depositForfeitureStatus", "PENDING_TENANT_CONFIRMATION");
            sourcePayload.remove("depositForfeitureConfirmedBy");
            sourcePayload.remove("depositForfeitureConfirmedAt");
            sourcePayload.remove("depositForfeitureDisputedBy");
            sourcePayload.remove("depositForfeitureDisputedAt");
            sourcePayload.remove("depositForfeitureDisputeReason");
            markChecklist(sourcePayload, "depositForfeitureConfirmed", false);
            sourcePayload.put("liquidationStage", "WAITING_DEPOSIT_FORFEITURE_CONFIRMATION");
            notifyTenant = true;
        } else if ("PENDING_TENANT_CONFIRMATION".equals(currentStatus)
                || "DISPUTED".equals(currentStatus)) {
            sourcePayload.put("liquidationStage", "WAITING_DEPOSIT_FORFEITURE_CONFIRMATION");
        }

        sourceRequest.setRequestPayload(toJson(sourcePayload));
        changeRequestRepository.save(sourceRequest);
        if (notifyTenant) {
            notifyTenantForfeitureConfirmation(sourceRequest, sourcePayload);
        }
        return toLiquidationDepositForfeitureLink(sourceRequest, sourcePayload);
    }

    @Transactional
    public void completeLiquidationRequest(Long contractId) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        if (sourceRequest == null || sourceRequest.getStatus() == RequestStatus.COMPLETED) {
            return;
        }
        Map<String, Object> payload = payloadMap(sourceRequest.getRequestPayload());
        payload.put("liquidationStage", "CONFIRMED");
        markChecklist(payload, "canConfirm", true);
        sourceRequest.setStatus(RequestStatus.COMPLETED);
        sourceRequest.setResolvedAt(LocalDateTime.now());
        sourceRequest.setRequestPayload(toJson(payload));
        changeRequestRepository.save(sourceRequest);
    }

    @Transactional
    public ExpenseRequestResponse markPaid(Long id, MarkExpensePaidRequest request, Long currentUserId, Role currentRole) {
        OperatingExpenseEntity expense = requireExpense(id);
        ExpenseApprovalRequestEntity approval = requireApproval(expense.getId());
        if (isLinkedLiquidationRefund(approval.getChangeRequest())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khoản hoàn cọc thanh lý chỉ cần khách thuê xác nhận đã nhận tiền; không dùng thao tác ghi nhận đã thanh toán."
            );
        }
        if (approval.getChangeRequest().getStatus() != RequestStatus.APPROVED
                || expense.getStatus() != ExpenseStatus.READY_FOR_PAYMENT) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (paymentRepository.existsByOperatingExpense_Id(expense.getId())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        if (currentRole != Role.OWNER
                && !(currentRole == Role.MANAGER && isLinkedLiquidationRefund(approval.getChangeRequest()))) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }

        UserEntity owner = requireUser(currentUserId);
        FileMetadataEntity receipt = resolveFile(request == null ? null : request.receiptFileId());
        LocalDateTime now = LocalDateTime.now();
        paymentRepository.save(ExpensePaymentEntity.builder()
                .operatingExpense(expense)
                .paymentDate(request == null || request.paymentDate() == null ? LocalDate.now() : request.paymentDate())
                .paymentMethod(request == null || request.paymentMethod() == null
                        ? ExpensePaymentMethod.CASH
                        : request.paymentMethod())
                .paymentReference(blankToNull(request == null ? null : request.paymentReference()))
                .receiptFile(receipt)
                .paidBy(owner)
                .paidAt(now)
                .note(blankToNull(request == null ? null : request.note()))
                .build());

        expense.setStatus(ExpenseStatus.PAID);
        expense.setPaidByUser(owner);
        expense.setReceiptFile(receipt);
        operatingExpenseRepository.save(expense);
        ChangeRequestEntity expenseChangeRequest = approval.getChangeRequest();
        expenseChangeRequest.setStatus(RequestStatus.COMPLETED);
        expenseChangeRequest.setResolvedBy(owner);
        expenseChangeRequest.setResolvedAt(now);
        changeRequestRepository.save(expenseChangeRequest);
        notifyRequester(expense, expenseChangeRequest, "EXPENSE_PAID");
        syncLiquidationDepositRefundRecorded(expense, expenseChangeRequest, request, receipt, owner, now);
        return toResponse(expense);
    }

    private Optional<ChangeRequestEntity> findLatestLiquidationRequest(Long contractId) {
        if (contractId == null) {
            return Optional.empty();
        }
        return changeRequestRepository.findFirstByRequestTypeAndTargetTypeAndTargetIdAndStatusInOrderByCreatedAtDesc(
                RequestType.CONTRACT_LIQUIDATION,
                TargetType.CONTRACT,
                contractId,
                LIQUIDATION_REQUEST_STATUSES
        );
    }

    private void updateLinkedRefundExpense(OperatingExpenseEntity expense, long refundAmount) {
        if (expense.getStatus() == ExpenseStatus.PAID && !Objects.equals(expense.getAmount(), refundAmount)) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        if (expense.getStatus() == ExpenseStatus.CANCELLED || expense.getStatus() == ExpenseStatus.REJECTED) {
            return;
        }
        if (!Objects.equals(expense.getAmount(), refundAmount)) {
            expense.setAmount(refundAmount);
            ExpenseApprovalRequestEntity approval = approvalRequestRepository
                    .findByOperatingExpense_Id(expense.getId())
                    .orElse(null);
            if (approval != null && expense.getStatus() == ExpenseStatus.READY_FOR_PAYMENT) {
                expense.setStatus(ExpenseStatus.PENDING_APPROVAL);
                approval.getChangeRequest().setStatus(RequestStatus.PENDING);
                approval.getChangeRequest().setResolvedBy(null);
                approval.getChangeRequest().setResolvedAt(null);
                changeRequestRepository.save(approval.getChangeRequest());
            }
            operatingExpenseRepository.save(expense);
        }
    }

    private void markRefundNotRequired(ChangeRequestEntity sourceRequest, Map<String, Object> payload) {
        Long existingExpenseId = toLong(payload.get("depositRefundExpenseId"));
        if (existingExpenseId != null) {
            OperatingExpenseEntity expense = operatingExpenseRepository.findById(existingExpenseId).orElse(null);
            if (expense != null && expense.getStatus() == ExpenseStatus.PAID) {
                throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
            }
            if (expense != null) {
                expense.setStatus(ExpenseStatus.CANCELLED);
                operatingExpenseRepository.save(expense);
                approvalRequestRepository.findByOperatingExpense_Id(expense.getId())
                        .map(ExpenseApprovalRequestEntity::getChangeRequest)
                        .ifPresent(request -> {
                            request.setStatus(RequestStatus.CANCELLED);
                            changeRequestRepository.save(request);
                        });
            }
        }
        payload.put("depositRefundStatus", "NOT_REQUIRED");
        payload.put("depositRefundAmount", 0L);
        payload.put("depositRefundedAmount", 0L);
        markChecklist(payload, "depositRefundConfirmed", true);
        payload.put("liquidationStage", liquidationStageAfterSettlementSync(
                payload,
                syncFinalInvoicePaid(payload),
                "NOT_REQUIRED"
        ));
        sourceRequest.setRequestPayload(toJson(payload));
        changeRequestRepository.save(sourceRequest);
    }

    private void syncLiquidationRefundPayload(
            ChangeRequestEntity sourceRequest,
            OperatingExpenseEntity expense,
            ExpenseApprovalRequestEntity approval,
            MarkExpensePaidRequest paymentRequest,
            FileMetadataEntity receipt,
            LocalDateTime paidAt
    ) {
        if (sourceRequest == null || expense == null) {
            return;
        }
        Map<String, Object> payload = payloadMap(sourceRequest.getRequestPayload());
        String currentStatus = Objects.toString(payload.get("depositRefundStatus"), "");
        String nextStatus = liquidationRefundStatus(expense.getStatus(), approval, currentStatus);
        payload.put("depositRefundStatus", nextStatus);
        payload.put("depositRefundAmount", expense.getAmount());
        payload.put("depositRefundExpenseId", expense.getId());
        payload.put("depositRefundExpenseStatus", expense.getStatus() == null ? null : expense.getStatus().name());
        if (approval != null && approval.getChangeRequest() != null) {
            payload.put("depositRefundExpenseRequestId", approval.getChangeRequest().getId());
            payload.put("depositRefundExpenseRequestCode", approval.getChangeRequest().getRequestCode());
            payload.put("depositRefundApprovalStatus", approval.getChangeRequest().getStatus() == null
                    ? null
                    : approval.getChangeRequest().getStatus().name());
        }
        if (paidAt != null) {
            payload.put("depositRefundedAmount", expense.getAmount());
            payload.put("depositRefundProofFileId", receipt == null ? null : receipt.getId());
            payload.put("depositRefundedBy", expense.getPaidByUser() == null ? null : expense.getPaidByUser().getId());
            payload.put("depositRefundedAt", paidAt.toString());
            payload.put("depositRefundMethod", paymentRequest == null || paymentRequest.paymentMethod() == null
                    ? ExpensePaymentMethod.CASH.name()
                    : paymentRequest.paymentMethod().name());
            payload.put("depositRefundTransactionRef", blankToNull(paymentRequest == null ? null : paymentRequest.paymentReference()));
            payload.put("depositRefundNote", blankToNull(paymentRequest == null ? null : paymentRequest.note()));
        }
        if (!"TENANT_CONFIRMED".equals(nextStatus)) {
            markChecklist(payload, "depositRefundConfirmed", false);
        }
        boolean finalInvoicePaid = syncFinalInvoicePaid(payload);
        payload.put("liquidationStage", liquidationStageAfterSettlementSync(payload, finalInvoicePaid, nextStatus));
        sourceRequest.setRequestPayload(toJson(payload));
        changeRequestRepository.save(sourceRequest);
        if (!Objects.equals(currentStatus, nextStatus)
                && "APPROVED_WAITING_TENANT_CONFIRMATION".equals(nextStatus)) {
            notifyTenantRefundApproved(sourceRequest, expense);
        }
    }

    @Transactional
    public void syncLiquidationFinalInvoicePaid(Long contractId) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        if (sourceRequest == null || sourceRequest.getStatus() == RequestStatus.COMPLETED) {
            return;
        }
        Map<String, Object> payload = payloadMap(sourceRequest.getRequestPayload());
        boolean finalInvoicePaid = syncFinalInvoicePaid(payload);
        if (finalInvoicePaid) {
            String refundStatus = Objects.toString(payload.get("depositRefundStatus"), "");
            payload.put("liquidationStage", liquidationStageAfterSettlementSync(payload, true, refundStatus));
            sourceRequest.setRequestPayload(toJson(payload));
            changeRequestRepository.save(sourceRequest);
        }
    }

    /**
     * Keeps the liquidation request checklist in sync with the canonical MOVE_OUT handover record.
     */
    @Transactional
    public void syncLiquidationHandoverConfirmed(Long contractId, Long handoverRecordId) {
        ChangeRequestEntity sourceRequest = findLatestLiquidationRequest(contractId).orElse(null);
        if (sourceRequest == null || sourceRequest.getStatus() == RequestStatus.COMPLETED) {
            return;
        }
        Map<String, Object> payload = payloadMap(sourceRequest.getRequestPayload());
        payload.put("handoverConfirmed", true);
        if (handoverRecordId != null) {
            payload.put("moveOutHandoverRecordId", handoverRecordId);
        }
        markChecklist(payload, "handoverConfirmed", true);
        boolean finalInvoicePaid = syncFinalInvoicePaid(payload);
        String refundStatus = Objects.toString(payload.get("depositRefundStatus"), "");
        payload.put("liquidationStage", liquidationStageAfterSettlementSync(payload, finalInvoicePaid, refundStatus));
        sourceRequest.setRequestPayload(toJson(payload));
        changeRequestRepository.save(sourceRequest);
    }

    private void syncLinkedLiquidationRefund(
            OperatingExpenseEntity expense,
            ChangeRequestEntity expenseChangeRequest
    ) {
        if (!isLinkedLiquidationRefund(expenseChangeRequest)) {
            return;
        }
        Map<String, Object> expensePayload = payloadMap(expenseChangeRequest.getRequestPayload());
        Long sourceRequestId = toLong(expensePayload.get("liquidationChangeRequestId"));
        if (sourceRequestId == null) {
            return;
        }
        changeRequestRepository.findById(sourceRequestId).ifPresent(sourceRequest ->
                syncLiquidationRefundPayload(
                        sourceRequest,
                        expense,
                        approvalRequestRepository.findByOperatingExpense_Id(expense.getId()).orElse(null),
                        null,
                        null,
                        null
                )
        );
    }

    private void syncLiquidationDepositRefundRecorded(
            OperatingExpenseEntity expense,
            ChangeRequestEntity expenseChangeRequest,
            MarkExpensePaidRequest paymentRequest,
            FileMetadataEntity receipt,
            UserEntity paidBy,
            LocalDateTime paidAt
    ) {
        if (!isLinkedLiquidationRefund(expenseChangeRequest)) {
            return;
        }
        Map<String, Object> expensePayload = payloadMap(expenseChangeRequest.getRequestPayload());
        Long sourceRequestId = toLong(expensePayload.get("liquidationChangeRequestId"));
        if (sourceRequestId == null) {
            return;
        }
        ChangeRequestEntity sourceRequest = changeRequestRepository.findById(sourceRequestId).orElse(null);
        if (sourceRequest == null) {
            return;
        }
        syncLiquidationRefundPayload(
                sourceRequest,
                expense,
                approvalRequestRepository.findByOperatingExpense_Id(expense.getId()).orElse(null),
                paymentRequest,
                receipt,
                paidAt
        );
        notifyTenantRefundRecorded(sourceRequest, expense, receipt, paidBy);
    }

    private void notifyTenantRefundRecorded(
            ChangeRequestEntity sourceRequest,
            OperatingExpenseEntity expense,
            FileMetadataEntity receipt,
            UserEntity paidBy
    ) {
        if (sourceRequest == null || sourceRequest.getRequester() == null) {
            return;
        }
        Map<String, Object> sourcePayload = payloadMap(sourceRequest.getRequestPayload());
        Long tenantUserId = toLong(sourcePayload.get("primaryTenantUserId"));
        if (tenantUserId == null) {
            tenantUserId = sourceRequest.getRequester().getId();
        }
        Map<String, Object> data = payload(
                "requestId", sourceRequest.getId(),
                "requestCode", sourceRequest.getRequestCode(),
                "contractId", sourcePayload.get("contractId"),
                "contractCode", sourcePayload.get("contractCode"),
                "roomCode", sourcePayload.get("roomCode"),
                "amount", expense.getAmount(),
                "depositRefundAmount", expense.getAmount(),
                "depositRefundProofFileId", receipt == null ? null : receipt.getId(),
                "paidBy", paidBy == null ? null : paidBy.getId(),
                "targetRoute", "/requests"
        );
        notificationPublisher.publish(
                "LIQUIDATION_DEPOSIT_REFUND_RECORDED",
                tenantUserId,
                "CHANGE_REQUEST",
                sourceRequest.getId(),
                data
        );
    }

    private void notifyTenantRefundApproved(
            ChangeRequestEntity sourceRequest,
            OperatingExpenseEntity expense
    ) {
        if (sourceRequest == null || expense == null) {
            return;
        }
        Map<String, Object> sourcePayload = payloadMap(sourceRequest.getRequestPayload());
        Long tenantUserId = toLong(sourcePayload.get("primaryTenantUserId"));
        if (tenantUserId == null && sourceRequest.getRequester() != null) {
            tenantUserId = sourceRequest.getRequester().getId();
        }
        if (tenantUserId == null) {
            return;
        }
        Map<String, Object> data = payload(
                "requestId", sourceRequest.getId(),
                "requestCode", sourceRequest.getRequestCode(),
                "contractId", sourcePayload.get("contractId"),
                "contractCode", sourcePayload.get("contractCode"),
                "roomCode", sourcePayload.get("roomCode"),
                "amount", expense.getAmount(),
                "depositRefundAmount", expense.getAmount(),
                "targetRoute", "/requests"
        );
        notificationPublisher.publish(
                "LIQUIDATION_DEPOSIT_REFUND_RECORDED",
                tenantUserId,
                "CHANGE_REQUEST",
                sourceRequest.getId(),
                data
        );
    }

    private boolean isLinkedLiquidationRefund(ChangeRequestEntity changeRequest) {
        if (changeRequest == null) {
            return false;
        }
        Map<String, Object> payload = payloadMap(changeRequest.getRequestPayload());
        return SOURCE_REQUEST_TYPE_CONTRACT_LIQUIDATION.equals(payload.get("sourceRequestType"))
                && toLong(payload.get("liquidationChangeRequestId")) != null;
    }

    static String liquidationRefundStatus(
            ExpenseStatus expenseStatus,
            ExpenseApprovalRequestEntity approval,
            String currentStatus
    ) {
        if ("TENANT_CONFIRMED".equals(currentStatus)) {
            return "TENANT_CONFIRMED";
        }
        if (expenseStatus == ExpenseStatus.PAID) {
            return "RECORDED_BY_MANAGER";
        }
        if (expenseStatus == ExpenseStatus.READY_FOR_PAYMENT) {
            return "APPROVED_WAITING_TENANT_CONFIRMATION";
        }
        if (expenseStatus == ExpenseStatus.REJECTED) {
            return "OWNER_REJECTED";
        }
        if (expenseStatus == ExpenseStatus.CANCELLED) {
            return "CANCELLED";
        }
        if (approval != null && approval.getChangeRequest() != null
                && approval.getChangeRequest().getStatus() == RequestStatus.PENDING) {
            return "WAITING_OWNER_APPROVAL";
        }
        return "PENDING";
    }

    private String liquidationStageAfterSettlementSync(
            Map<String, Object> payload,
            boolean finalInvoicePaid,
            String refundStatus
    ) {
        if (!isLiquidationHandoverConfirmed(payload)) {
            return "WAITING_HANDOVER";
        }
        String forfeitureStatus = Objects.toString(payload.get("depositForfeitureStatus"), "NOT_REQUIRED");
        if ("PENDING_TENANT_CONFIRMATION".equals(forfeitureStatus)
                || "DISPUTED".equals(forfeitureStatus)) {
            return "WAITING_DEPOSIT_FORFEITURE_CONFIRMATION";
        }
        if (!finalInvoicePaid) {
            return "WAITING_PAYMENT";
        }
        if (refundStatus == null
                || refundStatus.isBlank()
                || "TENANT_CONFIRMED".equals(refundStatus)
                || "NOT_REQUIRED".equals(refundStatus)) {
            return "READY_TO_COMPLETE";
        }
        return "WAITING_DEPOSIT_REFUND";
    }

    @SuppressWarnings("unchecked")
    private boolean isLiquidationHandoverConfirmed(Map<String, Object> payload) {
        if (Boolean.TRUE.equals(payload.get("handoverConfirmed"))) {
            return true;
        }
        Object rawChecklist = payload.get("liquidationChecklist");
        if (rawChecklist instanceof Map<?, ?> raw) {
            Object value = ((Map<String, Object>) raw).get("handoverConfirmed");
            return Boolean.TRUE.equals(value);
        }
        return false;
    }

    private void notifyTenantForfeitureConfirmation(
            ChangeRequestEntity sourceRequest,
            Map<String, Object> payload
    ) {
        Long tenantUserId = toLong(payload.get("primaryTenantUserId"));
        if (tenantUserId == null && sourceRequest.getRequester() != null) {
            tenantUserId = sourceRequest.getRequester().getId();
        }
        notificationPublisher.publish(
                "LIQUIDATION_DEPOSIT_FORFEITURE_CONFIRMATION_REQUIRED",
                tenantUserId,
                "CHANGE_REQUEST",
                sourceRequest.getId(),
                payload(
                        "requestId", sourceRequest.getId(),
                        "requestCode", sourceRequest.getRequestCode(),
                        "contractId", payload.get("contractId"),
                        "contractCode", payload.get("contractCode"),
                        "roomCode", payload.get("roomCode"),
                        "depositForfeitureAmount", payload.get("depositForfeitureAmount"),
                        "depositForfeitureReason", payload.get("depositForfeitureReason"),
                        "targetRoute", "/requests"
                )
        );
    }

    private boolean syncFinalInvoicePaid(Map<String, Object> payload) {
        Long contractId = toLong(payload.get("contractId"));
        InvoiceEntity invoice = contractId == null
                ? null
                : invoiceRepository
                .findFirstByLeastContract_IdAndInvoiceTypeAndStatusNotOrderByIdDesc(
                        contractId,
                        InvoiceType.FINAL_SETTLEMENT,
                        InvoiceStatus.VOIDED
                )
                .orElse(null);
        boolean paid = invoice != null
                && invoice.getStatus() == InvoiceStatus.PAID
                && safeAmount(invoice.getRemainingAmount()) <= 0;
        payload.put("finalInvoicePaid", paid);
        markChecklist(payload, "finalInvoicePaid", paid);
        if (invoice != null) {
            payload.put("finalInvoiceId", invoice.getId());
            payload.put("finalInvoiceCode", invoice.getInvoiceCode());
            payload.put("finalInvoiceRemainingAmount", safeAmount(invoice.getRemainingAmount()));
        }
        return paid;
    }

    private LiquidationDepositRefundLink toLiquidationDepositRefundLink(
            ChangeRequestEntity sourceRequest,
            Map<String, Object> payload
    ) {
        if (sourceRequest == null) {
            return LiquidationDepositRefundLink.empty();
        }
        return new LiquidationDepositRefundLink(
                sourceRequest.getId(),
                toLong(payload.get("depositRefundExpenseId")),
                toLong(payload.get("depositRefundExpenseRequestId")),
                payload.get("depositRefundStatus") == null ? null : payload.get("depositRefundStatus").toString(),
                toLong(payload.get("depositRefundProofFileId")),
                toLong(payload.get("depositRefundedAmount")),
                payload.get("depositRefundedAt") == null ? null : payload.get("depositRefundedAt").toString(),
                payload.get("depositRefundTransactionRef") == null ? null : payload.get("depositRefundTransactionRef").toString()
        );
    }

    private LiquidationDepositForfeitureLink toLiquidationDepositForfeitureLink(
            ChangeRequestEntity sourceRequest,
            Map<String, Object> payload
    ) {
        if (sourceRequest == null) {
            return LiquidationDepositForfeitureLink.empty();
        }
        return new LiquidationDepositForfeitureLink(
                sourceRequest.getId(),
                payload.get("depositForfeitureStatus") == null
                        ? null
                        : payload.get("depositForfeitureStatus").toString(),
                toLong(payload.get("depositForfeitureAmount")),
                payload.get("depositForfeitureReason") == null
                        ? null
                        : payload.get("depositForfeitureReason").toString(),
                toLong(payload.get("depositForfeitureConfirmedBy")),
                payload.get("depositForfeitureConfirmedAt") == null
                        ? null
                        : payload.get("depositForfeitureConfirmedAt").toString()
        );
    }

    private OperatingExpenseEntity updateExpenseStatus(
            OperatingExpenseEntity expense,
            ExpenseStatus status,
            UserEntity owner,
            LocalDateTime approvedAt
    ) {
        expense.setStatus(status);
        expense.setApprovedBy(owner);
        expense.setApprovedAt(approvedAt);
        return expense;
    }

    private void requirePending(OperatingExpenseEntity expense, ChangeRequestEntity changeRequest) {
        if (expense.getStatus() != ExpenseStatus.PENDING_APPROVAL || changeRequest.getStatus() != RequestStatus.PENDING) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private void saveAttachments(OperatingExpenseEntity expense, List<CreateExpenseAttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (CreateExpenseAttachmentRequest attachment : attachments) {
            if (attachment == null || attachment.fileId() == null) {
                continue;
            }
            FileMetadataEntity file = resolveFile(attachment.fileId());
            attachmentRepository.save(ExpenseAttachmentEntity.builder()
                    .operatingExpense(expense)
                    .file(file)
                    .attachmentType(attachment.attachmentType() == null ? ExpenseAttachmentType.OTHER : attachment.attachmentType())
                    .build());
        }
    }

    private ExpenseRequestResponse toResponse(OperatingExpenseEntity expense) {
        ExpenseApprovalRequestEntity approval = approvalRequestRepository.findByOperatingExpense_Id(expense.getId())
                .orElse(null);
        ChangeRequestEntity changeRequest = approval == null ? null : approval.getChangeRequest();
        ExpensePaymentEntity payment = paymentRepository.findByOperatingExpense_Id(expense.getId()).orElse(null);

        return new ExpenseRequestResponse(
                expense.getId(),
                expense.getExpenseCode(),
                expense.getExpenseType() == null ? null : expense.getExpenseType().name(),
                expense.getStatus() == null ? null : expense.getStatus().name(),
                changeRequest == null || changeRequest.getStatus() == null ? null : changeRequest.getStatus().name(),
                payment == null ? "UNPAID" : "PAID",
                expense.getProperty() == null ? null : expense.getProperty().getId(),
                expense.getProperty() == null ? null : expense.getProperty().getName(),
                expense.getRoom() == null ? null : expense.getRoom().getId(),
                expense.getRoom() == null ? null : expense.getRoom().getRoomCode(),
                expense.getAmount(),
                expense.getDescription(),
                approval == null ? null : approval.getReason(),
                approval == null ? null : approval.getVendorName(),
                approval == null ? null : approval.getExpectedPaymentDate(),
                expense.getExpenseDate(),
                changeRequest == null ? null : changeRequest.getId(),
                changeRequest == null ? null : changeRequest.getRequestCode(),
                changeRequest == null || changeRequest.getRequester() == null ? null : changeRequest.getRequester().getId(),
                expense.getApprovedBy() == null ? null : expense.getApprovedBy().getId(),
                expense.getApprovedAt(),
                toPaymentResponse(payment),
                toAttachmentResponses(expense.getId()),
                toTimeline(changeRequest, payment),
                expense.getCreatedAt()
        );
    }

    private ExpensePaymentResponse toPaymentResponse(ExpensePaymentEntity payment) {
        if (payment == null) {
            return null;
        }
        return new ExpensePaymentResponse(
                payment.getId(),
                payment.getPaymentDate(),
                payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().name(),
                payment.getPaymentReference(),
                payment.getReceiptFile() == null ? null : payment.getReceiptFile().getId(),
                payment.getPaidBy() == null ? null : payment.getPaidBy().getId(),
                payment.getPaidAt(),
                payment.getNote()
        );
    }

    private List<ExpenseAttachmentResponse> toAttachmentResponses(Long expenseId) {
        return attachmentRepository.findAllByOperatingExpense_IdOrderByIdAsc(expenseId).stream()
                .map(attachment -> new ExpenseAttachmentResponse(
                        attachment.getId(),
                        attachment.getFile() == null ? null : attachment.getFile().getId(),
                        attachment.getFile() == null ? null : attachment.getFile().getOriginalName(),
                        attachment.getAttachmentType() == null ? null : attachment.getAttachmentType().name(),
                        attachment.getCreatedAt()
                ))
                .toList();
    }

    private List<ExpenseTimelineResponse> toTimeline(ChangeRequestEntity changeRequest, ExpensePaymentEntity payment) {
        List<ExpenseTimelineResponse> timeline = new ArrayList<>();
        if (changeRequest != null) {
            changeRequestEventRepository.findAllByChangeRequest_Id(changeRequest.getId()).stream()
                    .map(event -> new ExpenseTimelineResponse(
                            event.getFromStatus() == null ? null : event.getFromStatus().name(),
                            event.getToStatus() == null ? null : event.getToStatus().name(),
                            event.getNote(),
                            event.getActedBy() == null ? null : event.getActedBy().getId(),
                            event.getActedAt()
                    ))
                    .forEach(timeline::add);
        }
        if (payment != null) {
            timeline.add(new ExpenseTimelineResponse(
                    "READY_FOR_PAYMENT",
                    "PAID",
                    payment.getNote(),
                    payment.getPaidBy() == null ? null : payment.getPaidBy().getId(),
                    payment.getPaidAt()
            ));
        }
        timeline.sort(Comparator.comparing(ExpenseTimelineResponse::actedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    private void recordChangeEvent(
            ChangeRequestEntity changeRequest,
            RequestStatus fromStatus,
            RequestStatus toStatus,
            String note,
            Long actedBy
    ) {
        changeRequestEventRepository.save(ChangeRequestEventEntity.builder()
                .changeRequest(changeRequest)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .note(note)
                .actedBy(actedBy == null ? null : userRepository.getReferenceById(actedBy))
                .build());
    }

    private void notifyOwners(OperatingExpenseEntity expense, ChangeRequestEntity changeRequest) {
        List<Long> ownerIds = userRepository.findIdsByRolesAndStatus(List.of(Role.OWNER), AccountStatus.ACTIVE);
        for (Long ownerId : ownerIds) {
            queueNotification(ownerId, "EXPENSE_APPROVAL_REQUESTED", expense, changeRequest);
        }
    }

    private void notifyRequester(
            OperatingExpenseEntity expense,
            ChangeRequestEntity changeRequest,
            String eventType
    ) {
        if (changeRequest == null || changeRequest.getRequester() == null) {
            return;
        }
        queueNotification(changeRequest.getRequester().getId(), eventType, expense, changeRequest);
    }

    private void queueNotification(
            Long recipientUserId,
            String eventType,
            OperatingExpenseEntity expense,
            ChangeRequestEntity changeRequest
    ) {
        if (recipientUserId == null) {
            return;
        }
        Map<String, Object> data = payload(
                "operatingExpenseId", expense.getId(),
                "expenseCode", expense.getExpenseCode(),
                "changeRequestId", changeRequest == null ? null : changeRequest.getId(),
                "requestCode", changeRequest == null ? null : changeRequest.getRequestCode(),
                "expenseType", expense.getExpenseType() == null ? null : expense.getExpenseType().name(),
                "amount", expense.getAmount(),
                "description", expense.getDescription(),
                "propertyId", expense.getProperty() == null ? null : expense.getProperty().getId(),
                "propertyName", expense.getProperty() == null ? null : expense.getProperty().getName(),
                "roomId", expense.getRoom() == null ? null : expense.getRoom().getId(),
                "roomCode", expense.getRoom() == null ? null : expense.getRoom().getRoomCode(),
                "resolutionNote", changeRequest == null ? null : changeRequest.getResolutionNote(),
                "targetRoute", "/dashboard/expense-requests/" + expense.getId(),
                "status", expense.getStatus() == null ? null : expense.getStatus().name()
        );
        notificationPublisher.publish(eventType, recipientUserId, "EXPENSE_REQUEST", expense.getId(), data);
    }

    private String nextRequestCode(RequestType requestType, String roomCode) {
        return RequestCodeBuilder.nextAvailable(
                requestType,
                roomCode,
                LocalDate.now(),
                changeRequestRepository::existsByRequestCode
        );
    }

    private OperatingExpenseEntity requireExpense(Long id) {
        if (id == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return operatingExpenseRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
    }

    private ExpenseApprovalRequestEntity requireApproval(Long expenseId) {
        return approvalRequestRepository.findByOperatingExpense_Id(expenseId)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
    }

    private UserEntity requireUser(Long id) {
        if (id == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNAUTHENTICATED));
    }

    private RoomEntity resolveRoom(Long roomId, Long propertyId) {
        if (roomId == null) {
            return null;
        }
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        if (room.getProperty() == null || !Objects.equals(room.getProperty().getId(), propertyId)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return room;
    }

    private FileMetadataEntity resolveFile(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
    }

    private long requirePositiveAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return amount;
    }

    private RequesterRole toRequesterRole(Role role) {
        return switch (role) {
            case OWNER -> RequesterRole.OWNER;
            case MANAGER -> RequesterRole.MANAGER;
            case ACCOUNTANT -> RequesterRole.ACCOUNTANT;
            case TENANT -> RequesterRole.TENANT;
            case LEAD -> RequesterRole.LEAD;
        };
    }

    private Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> value = new LinkedHashMap<>();
        if (keyValues == null) {
            return value;
        }
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object key = keyValues[index];
            Object item = keyValues[index + 1];
            if (key != null && item != null) {
                value.put(key.toString(), item);
            }
        }
        return value;
    }

    private Map<String, Object> payloadMap(String payloadJson) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (payloadJson == null || payloadJson.isBlank()) {
            return data;
        }
        try {
            data.putAll(objectMapper.readValue(
                    payloadJson,
                    new TypeReference<Map<String, Object>>() {
                    }
            ));
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST, e);
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private void markChecklist(Map<String, Object> payload, String key, boolean value) {
        Object rawChecklist = payload.get("liquidationChecklist");
        Map<String, Object> checklist = rawChecklist instanceof Map<?, ?> raw
                ? new LinkedHashMap<>((Map<String, Object>) raw)
                : new LinkedHashMap<>();
        checklist.put(key, value);
        payload.put("liquidationChecklist", checklist);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeKeyword(String keyword) {
        String value = blankToNull(keyword);
        return value == null ? null : value.toLowerCase();
    }

    private String defaultText(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private long safeAmount(Long amount) {
        return amount == null ? 0L : Math.max(0L, amount);
    }

    public record LiquidationDepositRefundLink(
            Long liquidationChangeRequestId,
            Long expenseId,
            Long expenseRequestId,
            String status,
            Long proofFileId,
            Long refundedAmount,
            String refundedAt,
            String transactionRef
    ) {
        public static LiquidationDepositRefundLink empty() {
            return new LiquidationDepositRefundLink(null, null, null, null, null, null, null, null);
        }
    }

    public record LiquidationDepositForfeitureLink(
            Long liquidationChangeRequestId,
            String status,
            Long amount,
            String reason,
            Long confirmedBy,
            String confirmedAt
    ) {
        public static LiquidationDepositForfeitureLink empty() {
            return new LiquidationDepositForfeitureLink(null, null, null, null, null, null);
        }
    }
}
