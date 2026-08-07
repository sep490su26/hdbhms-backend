package com.sep490.hdbhms.booking.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.booking.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.property.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.booking.application.port.in.usecase.GetDepositAgreementDetailsUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.booking.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.booking.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.property.application.port.out.FloorRepository;
import com.sep490.hdbhms.property.application.port.out.PropertyRepository;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.booking.application.service.DepositAgreementLifecycleService;
import com.sep490.hdbhms.booking.application.service.DepositLifecyclePolicy;
import com.sep490.hdbhms.booking.domain.model.DepositAgreement;
import com.sep490.hdbhms.booking.domain.model.DepositForm;
import com.sep490.hdbhms.property.domain.model.Floor;
import com.sep490.hdbhms.property.domain.model.Property;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.booking.infrastructure.web.dto.request.DepositAgreementManagementUpdateRequest;
import com.sep490.hdbhms.booking.infrastructure.web.dto.request.DepositAgreementStatusUpdateRequest;
import com.sep490.hdbhms.booking.infrastructure.web.dto.request.DepositContactRequest;
import com.sep490.hdbhms.booking.infrastructure.web.dto.request.DepositExtensionRequest;
import com.sep490.hdbhms.booking.infrastructure.web.dto.request.DepositForfeitureRequest;
import com.sep490.hdbhms.booking.infrastructure.web.dto.response.DepositAgreementDetailsResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit-agreements")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositAgreementController {
    private static final java.time.format.DateTimeFormatter DOCUMENT_FILENAME_DATE_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("dd_MM_yyyy");

    private static final Set<DepositAgreementStatus> MANAGER_UPDATEABLE_STATUSES = EnumSet.of(
            DepositAgreementStatus.CONVERTED_TO_LEASE,
            DepositAgreementStatus.REFUNDED
    );
    private static final Set<DepositAgreementStatus> MANAGER_INFO_UPDATEABLE_STATUSES = EnumSet.of(
            DepositAgreementStatus.PENDING_PAYMENT,
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONFIRMED,
            DepositAgreementStatus.EXTENDED
    );

    GetRoomDetailsUseCase getRoomDetailsUseCase;
    PropertyRepository propertyRepository;
    DepositFormRepository depositFormRepository;
    FloorRepository floorRepository;
    DepositAgreementRepository depositAgreementRepository;
    RoomRepository roomRepository;
    GetDepositAgreementDetailsUseCase getDepositAgreementDetailsUseCase;
    DepositAgreementLifecycleService depositAgreementLifecycleService;

    @GetMapping("/{depositAgreementId}")
    public ApiResponse<DepositAgreementDetailsResponse> getDepositAgreementDetails(
            @PathVariable("depositAgreementId") Long depositAgreementId
    ) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        assertCanAccessDeposit(depositAgreement);
        Room room = getRoomDetailsUseCase.execute(
                new GetRoomDetailsQuery(depositAgreement.getRoomId())
        );
        return ApiResponse.<DepositAgreementDetailsResponse>builder()
                .data(toDetailsResponse(depositAgreement, room))
                .build();
    }

    @PatchMapping("/{depositAgreementId}/status")
    public ApiResponse<DepositAgreementDetailsResponse> updateDepositAgreementStatus(
            @PathVariable("depositAgreementId") Long depositAgreementId,
            @Valid @RequestBody DepositAgreementStatusUpdateRequest request
    ) {
        assertOwnerOrManager();
        DepositAgreementStatus nextStatus = request.status();
        if (!MANAGER_UPDATEABLE_STATUSES.contains(nextStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái cọc không hợp lệ. Mất cọc phải được xử lý qua hành động có kiểm tra quá hạn."
            );
        }

        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        Room room = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(depositAgreement.getRoomId()));

        depositAgreement.changeStatus(nextStatus);
        updateRoomStatusForDepositStatus(room, nextStatus);

        DepositAgreement savedDepositAgreement = depositAgreementRepository.save(depositAgreement);
        Room savedRoom = roomRepository.save(room);

        return ApiResponse.<DepositAgreementDetailsResponse>builder()
                .data(toDetailsResponse(savedDepositAgreement, savedRoom))
                .build();
    }

    @PatchMapping("/{depositAgreementId}/management-info")
    public ApiResponse<DepositAgreementDetailsResponse> updateDepositAgreementManagementInfo(
            @PathVariable("depositAgreementId") Long depositAgreementId,
            @Valid @RequestBody DepositAgreementManagementUpdateRequest request
    ) {
        assertOwnerOrManager();

        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        DepositForm currentDepositForm = getDepositForm(depositAgreement);
        LocalDate currentMoveInDate = resolveExpectedMoveInDate(depositAgreement, currentDepositForm);
        LocalDate currentLeaseSignDate = resolveExpectedLeaseSignDate(depositAgreement, currentDepositForm);
        if (depositAgreement.getStatus() != DepositAgreementStatus.PENDING_PAYMENT
                && (!Objects.equals(currentMoveInDate, request.expectedMoveInDate())
                || !Objects.equals(currentLeaseSignDate, request.expectedLeaseSignDate()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày hẹn của cọc đã thanh toán chỉ được thay đổi qua hành động gia hạn."
            );
        }
        validateManagementInfoUpdate(
                request,
                depositAgreement.getStatus() == DepositAgreementStatus.PENDING_PAYMENT
        );
        if (!MANAGER_INFO_UPDATEABLE_STATUSES.contains(depositAgreement.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được cập nhật thông tin khi cọc đang chờ thanh toán, đã đặt cọc, đang giữ cọc hoặc chờ ký hợp đồng."
            );
        }

        DepositForm depositForm = getDepositForm(depositAgreement);
        if (depositForm == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy form đặt cọc để cập nhật thông tin khách.");
        }

        String normalizedPhone = normalizePhone(request.depositorPhone());
        depositForm.updateManagerEditableInfo(
                normalizedPhone,
                request.permanentAddress().trim(),
                request.expectedMoveInDate(),
                request.expectedLeaseSignDate()
        );
        depositAgreement.updateExpectedDates(request.expectedMoveInDate(), request.expectedLeaseSignDate());

        depositFormRepository.save(depositForm);
        DepositAgreement savedDepositAgreement = depositAgreementRepository.save(depositAgreement);
        Room room = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(savedDepositAgreement.getRoomId()));

        return ApiResponse.<DepositAgreementDetailsResponse>builder()
                .data(toDetailsResponse(savedDepositAgreement, room))
                .build();
    }

    @PostMapping("/{depositAgreementId}/contact-events")
    public ApiResponse<DepositAgreementDetailsResponse> recordDepositContact(
            @PathVariable Long depositAgreementId,
            @Valid @RequestBody DepositContactRequest request
    ) {
        assertOwnerOrManager();
        depositAgreementLifecycleService.recordContact(
                depositAgreementId,
                AuthUtils.getCurrentAuthenticationId(),
                request.outcome(),
                request.note()
        );
        return currentDetailsResponse(depositAgreementId);
    }

    @PostMapping("/{depositAgreementId}/extensions")
    public ApiResponse<DepositAgreementDetailsResponse> extendDeposit(
            @PathVariable Long depositAgreementId,
            @Valid @RequestBody DepositExtensionRequest request
    ) {
        assertOwnerOrManager();
        depositAgreementLifecycleService.extend(
                depositAgreementId,
                AuthUtils.getCurrentAuthenticationId(),
                request.additionalDays(),
                request.reason()
        );
        return currentDetailsResponse(depositAgreementId);
    }

    @PostMapping("/{depositAgreementId}/forfeit")
    public ApiResponse<DepositAgreementDetailsResponse> forfeitDeposit(
            @PathVariable Long depositAgreementId,
            @Valid @RequestBody DepositForfeitureRequest request
    ) {
        assertOwner();
        depositAgreementLifecycleService.forfeit(depositAgreementId, request.reason());
        return currentDetailsResponse(depositAgreementId);
    }

    private DepositAgreementDetailsResponse toDetailsResponse(DepositAgreement depositAgreement, Room room) {
        Property property = propertyRepository.findById(room.getPropertyId()).orElse(null);
        Floor floor = floorRepository.findById(room.getFloorId()).orElse(null);
        DepositForm depositForm = getDepositForm(depositAgreement);
        var lifecycle = depositAgreementLifecycleService.snapshot(depositAgreement.getId());
        return DepositAgreementDetailsResponse.builder()
                .id(depositAgreement.getId())
                .depositCode(depositAgreement.getDepositCode())
                .roomCode(room.getRoomCode())
                .propertyName(property != null ? property.getName() : null)
                .propertyAddress(property != null ? property.getAddressLine() : null)
                .floorId(room.getFloorId())
                .floorName(floor != null ? floor.getName() : null)
                .depositorFullName(depositForm != null ? depositForm.getFullName() : null)
                .depositorPhone(depositForm != null ? depositForm.getPhone() : null)
                .depositorEmail(depositForm != null ? depositForm.getEmail() : null)
                .depositorPermanentAddress(depositForm != null ? depositForm.getPermanentAddress() : null)
                .amount(depositAgreement.getAmount())
                .expectedMoveInDate(resolveExpectedMoveInDate(depositAgreement, depositForm))
                .expectedLeaseSignDate(resolveExpectedLeaseSignDate(depositAgreement, depositForm))
                .depositExpiresAt(depositAgreement.getDepositExpiresAt())
                .status(depositAgreement.getStatus())
                .confirmedAt(depositAgreement.getConfirmedAt())
                .idFrontFileId(depositForm != null ? depositForm.getIdFrontFileId() : null)
                .idFrontFileUrl(fileDownloadUrl(depositForm != null ? depositForm.getIdFrontFileId() : null))
                .idBackFileId(depositForm != null ? depositForm.getIdBackFileId() : null)
                .idBackFileUrl(fileDownloadUrl(depositForm != null ? depositForm.getIdBackFileId() : null))
                .portraitFileId(depositForm != null ? depositForm.getPortraitFileId() : null)
                .portraitFileUrl(fileDownloadUrl(depositForm != null ? depositForm.getPortraitFileId() : null))
                .note(depositAgreement.getNote())
                .createdAt(depositAgreement.getCreatedAt())
                .extensionCount(lifecycle.extensionCount())
                .maxExtensions(lifecycle.maxExtensions())
                .depositExpiresAt(lifecycle.depositExpiresAt())
                .forfeitureDecisionDate(lifecycle.forfeitureDecisionDate())
                .overdueDays(lifecycle.overdueDays())
                .latestContactOutcome(lifecycle.latestContactOutcome())
                .lastContactedAt(lifecycle.lastContactedAt())
                .lastContactNote(lifecycle.lastContactNote())
                .contactRequired(lifecycle.contactRequired())
                .canExtend(lifecycle.canExtend())
                .canForfeit(lifecycle.forfeitureEligible() && currentRole() == Role.OWNER)
                .build();
    }

    private DepositForm getDepositForm(DepositAgreement depositAgreement) {
        if (depositAgreement.getDepositFormId() == null) {
            return null;
        }
        return depositFormRepository.findById(depositAgreement.getDepositFormId()).orElse(null);
    }

    private String fileDownloadUrl(Long fileId) {
        return fileId == null ? null : "/api/v1/files/private/" + fileId;
    }

    private java.time.LocalDate resolveExpectedMoveInDate(DepositAgreement depositAgreement, DepositForm depositForm) {
        if (depositAgreement.getExpectedMoveInDate() != null) {
            return depositAgreement.getExpectedMoveInDate();
        }
        return depositForm != null ? depositForm.getExpectedMoveInDate() : null;
    }

    private java.time.LocalDate resolveExpectedLeaseSignDate(DepositAgreement depositAgreement, DepositForm depositForm) {
        if (depositAgreement.getExpectedLeaseSignDate() != null) {
            return depositAgreement.getExpectedLeaseSignDate();
        }
        return depositForm != null ? depositForm.getExpectedLeaseSignDate() : null;
    }

    private void validateManagementInfoUpdate(
            DepositAgreementManagementUpdateRequest request,
            boolean scheduleChangesAllowed
    ) {
        String normalizedPhone = normalizePhone(request.depositorPhone());
        if (!normalizedPhone.matches("^0\\d{9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số.");
        }
        if (request.permanentAddress() == null || request.permanentAddress().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không được để trống.");
        }

        LocalDate today = LocalDate.now();
        if (scheduleChangesAllowed && request.expectedLeaseSignDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày ký hợp đồng dự kiến không được là ngày quá khứ.");
        }
        if (scheduleChangesAllowed && request.expectedMoveInDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày vào ở dự kiến không được là ngày quá khứ.");
        }
        if (request.expectedMoveInDate().isBefore(request.expectedLeaseSignDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày vào ở dự kiến không được trước ngày ký hợp đồng dự kiến.");
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s.\\-]", "");
    }

    private void updateRoomStatusForDepositStatus(Room room, DepositAgreementStatus nextStatus) {
        if (nextStatus == DepositAgreementStatus.CONVERTED_TO_LEASE) {
            room.occupyRoom();
            return;
        }
        room.reserveRoom();
    }

    private void assertOwnerOrManager() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để cập nhật trạng thái cọc.");
        }

        Role role = principal.getRole();
        if (role != Role.OWNER && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật trạng thái cọc.");
        }
    }

    private void assertOwner() {
        if (currentRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ trọ được quyết định xử lý mất cọc.");
        }
    }

    private Role currentRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getRole();
    }

    private ApiResponse<DepositAgreementDetailsResponse> currentDetailsResponse(Long depositAgreementId) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        if (!DepositLifecyclePolicy.isActive(depositAgreement.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể thay đổi khoản cọc đã kết thúc xử lý.");
        }
        Room room = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(depositAgreement.getRoomId()));
        return ApiResponse.<DepositAgreementDetailsResponse>builder()
                .data(toDetailsResponse(depositAgreement, room))
                .build();
    }

    private void assertCanAccessDeposit(DepositAgreement depositAgreement) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem hợp đồng đặt cọc.");
        }

        Role role = principal.getRole();
        if (role == Role.OWNER || role == Role.MANAGER) {
            return;
        }

        if (role == Role.TENANT) {
            boolean canAccess = depositAgreementRepository.findAllAccessibleByUserId(principal.getId()).stream()
                    .anyMatch(agreement -> agreement.getId().equals(depositAgreement.getId()));
            if (canAccess) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hợp đồng đặt cọc này.");
    }
}
