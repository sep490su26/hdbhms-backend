package com.sep490.hdbhms.accounting.application.service;

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
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseRequestService {

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tạo yêu cầu chi.");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu chi không hợp lệ.");
        }
        if (request.propertyId() == null || request.expenseType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn cơ sở và loại chi.");
        }
        String reason = blankToNull(request.reason());
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do chi.");
        }
        long amount = requirePositiveAmount(request.amount());
        UserEntity requester = requireUser(currentUserId);
        PropertyEntity property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy cơ sở."));
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
                .requestCode("CR-" + snowflakeIdGenerator.next())
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
                "Owner duyệt yêu cầu chi, chờ thanh toán", ownerId);
        notifyRequester(expense, changeRequest, "EXPENSE_APPROVED");
        return toResponse(expense);
    }

    @Transactional
    public ExpenseRequestResponse rejectRequest(Long id, RejectExpenseRequest request, Long ownerId) {
        String reason = blankToNull(request == null ? null : request.reason());
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do từ chối.");
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
        return toResponse(expense);
    }

    @Transactional
    public ExpenseRequestResponse cancelRequest(Long id, Long currentUserId, Role currentRole) {
        OperatingExpenseEntity expense = requireExpense(id);
        ExpenseApprovalRequestEntity approval = requireApproval(expense.getId());
        ChangeRequestEntity changeRequest = approval.getChangeRequest();
        requirePending(expense, changeRequest);
        if (currentRole != Role.OWNER && !Objects.equals(changeRequest.getRequester().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy yêu cầu chi này.");
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

    @Transactional
    public ExpenseRequestResponse markPaid(Long id, MarkExpensePaidRequest request, Long ownerId) {
        OperatingExpenseEntity expense = requireExpense(id);
        ExpenseApprovalRequestEntity approval = requireApproval(expense.getId());
        if (approval.getChangeRequest().getStatus() != RequestStatus.APPROVED
                || expense.getStatus() != ExpenseStatus.READY_FOR_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ ghi nhận thanh toán cho yêu cầu chi đã được duyệt.");
        }
        if (paymentRepository.existsByOperatingExpense_Id(expense.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu chi này đã được ghi nhận thanh toán.");
        }

        UserEntity owner = requireUser(ownerId);
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
        notifyRequester(expense, approval.getChangeRequest(), "EXPENSE_PAID");
        return toResponse(expense);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ xử lý yêu cầu chi đang chờ duyệt.");
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

    private OperatingExpenseEntity requireExpense(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn yêu cầu chi.");
        }
        return operatingExpenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu chi."));
    }

    private ExpenseApprovalRequestEntity requireApproval(Long expenseId) {
        return approvalRequestRepository.findByOperatingExpense_Id(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin phê duyệt."));
    }

    private UserEntity requireUser(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ.");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ."));
    }

    private RoomEntity resolveRoom(Long roomId, Long propertyId) {
        if (roomId == null) {
            return null;
        }
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng."));
        if (room.getProperty() == null || !Objects.equals(room.getProperty().getId(), propertyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phòng không thuộc cơ sở đã chọn.");
        }
        return room;
    }

    private FileMetadataEntity resolveFile(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một hoặc nhiều file không tồn tại."));
    }

    private long requirePositiveAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền chi không hợp lệ.");
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
}
