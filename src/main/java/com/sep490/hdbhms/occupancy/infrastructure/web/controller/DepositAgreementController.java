package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListDepositAgreementsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetDepositAgreementDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListDepositAgreementsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.DepositAgreementManagementUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.DepositAgreementStatusUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deposit-agreements")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositAgreementController {
    private static final Set<DepositAgreementStatus> MANAGER_UPDATEABLE_STATUSES = EnumSet.of(
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONVERTED_TO_LEASE,
            DepositAgreementStatus.REFUNDED,
            DepositAgreementStatus.FORFEITED
    );
    private static final Set<DepositAgreementStatus> MANAGER_INFO_UPDATEABLE_STATUSES = EnumSet.of(
            DepositAgreementStatus.PENDING_PAYMENT,
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONFIRMED,
            DepositAgreementStatus.EXTENDED
    );

    GetRoomDetailsUseCase getRoomDetailsUseCase;
    TenantRepository tenantRepository;
    PropertyRepository propertyRepository;
    DepositFormRepository depositFormRepository;
    DepositAgreementRepository depositAgreementRepository;
    RoomRepository roomRepository;
    GetMyListDepositAgreementsUseCase getMyListDepositAgreementsUseCase;
    GetDepositAgreementDetailsUseCase getDepositAgreementDetailsUseCase;
    DepositContractDocumentService depositContractDocumentService;

    @GetMapping
    public ApiResponse<PageResponse<DepositAgreementResponse>> getDepositAgreements(
            @RequestParam(required = false) DepositAgreementStatus status,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return listDepositAgreements(status, signedFrom, signedTo, pageable);
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<DepositAgreementResponse>> getMyDepositAgreements(
            @RequestParam(required = false) DepositAgreementStatus status,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return listDepositAgreements(status, signedFrom, signedTo, pageable);
    }

    private ApiResponse<PageResponse<DepositAgreementResponse>> listDepositAgreements(
            DepositAgreementStatus status,
            LocalDateTime signedFrom,
            LocalDateTime signedTo,
            Pageable pageable
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<PageResponse<DepositAgreementResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getMyListDepositAgreementsUseCase.execute(
                                        new GetListDepositAgreementsQuery(
                                                userId,
                                                status,
                                                signedFrom,
                                                signedTo,
                                                pageable
                                        )
                                ).map(this::toListResponse)
                        )
                )
                .build();
    }

    @GetMapping("/{depositAgreementId}")
    public ApiResponse<DepositAgreementDetailsResponse> getDepositAgreementDetails(
            @PathVariable("depositAgreementId") Long depositAgreementId
    ) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        assertCanAccessContract(depositAgreement);
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
                    "Trạng thái cọc không hợp lệ. Chỉ hỗ trợ: đã đặt cọc, đã nhận phòng, đã hoàn cọc, mất cọc."
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
        validateManagementInfoUpdate(request);

        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
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

        depositContractDocumentService.regenerateOfficialContractAfterCommit(savedDepositAgreement.getId());

        return ApiResponse.<DepositAgreementDetailsResponse>builder()
                .data(toDetailsResponse(savedDepositAgreement, room))
                .build();
    }

    @GetMapping("/{depositAgreementId}/contract")
    public ResponseEntity<Resource> downloadDepositContract(
            @PathVariable("depositAgreementId") Long depositAgreementId
    ) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        assertCanAccessContract(depositAgreement);

        FileDataResponse fileData = depositContractDocumentService.getOfficialContractFile(depositAgreementId);
        String contentType = fileData.contentType() == null
                ? MediaType.APPLICATION_PDF_VALUE
                : fileData.contentType();
        String filename = "deposit-contract-" + depositAgreement.getDepositCode() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(fileData.resource());
    }

    private DepositAgreementResponse toListResponse(DepositAgreement depositAgreement) {
        Room room = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(depositAgreement.getRoomId()));
        Property property = propertyRepository.findById(room.getPropertyId()).orElse(null);
        DepositForm depositForm = getDepositForm(depositAgreement);
        return DepositAgreementResponse.builder()
                .id(depositAgreement.getId())
                .depositCode(depositAgreement.getDepositCode())
                .roomCode(room.getRoomCode())
                .propertyName(property != null ? property.getName() : null)
                .depositorFullName(depositForm != null ? depositForm.getFullName() : null)
                .depositorPhone(depositForm != null ? depositForm.getPhone() : null)
                .depositorEmail(depositForm != null ? depositForm.getEmail() : null)
                .amount(depositAgreement.getAmount())
                .expectedMoveInDate(resolveExpectedMoveInDate(depositAgreement, depositForm))
                .expectedLeaseSignDate(resolveExpectedLeaseSignDate(depositAgreement, depositForm))
                .createdAt(depositAgreement.getCreatedAt())
                .status(depositAgreement.getStatus())
                .confirmedAt(depositAgreement.getConfirmedAt())
                .contractFileId(depositAgreement.getContractFileId())
                .contractDownloadUrl("/api/v1/deposit-agreements/" + depositAgreement.getId() + "/contract")
                .build();
    }

    private DepositAgreementDetailsResponse toDetailsResponse(DepositAgreement depositAgreement, Room room) {
        Property property = propertyRepository.findById(room.getPropertyId()).orElse(null);
        DepositForm depositForm = getDepositForm(depositAgreement);
        return DepositAgreementDetailsResponse.builder()
                .id(depositAgreement.getId())
                .depositCode(depositAgreement.getDepositCode())
                .roomCode(room.getRoomCode())
                .propertyName(property != null ? property.getName() : null)
                .propertyAddress(property != null ? property.getAddressLine() : null)
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
                .contractFileId(depositAgreement.getContractFileId())
                .contractDownloadUrl("/api/v1/deposit-agreements/" + depositAgreement.getId() + "/contract")
                .idFrontFileId(depositForm != null ? depositForm.getIdFrontFileId() : null)
                .idFrontFileUrl(fileDownloadUrl(depositForm != null ? depositForm.getIdFrontFileId() : null))
                .idBackFileId(depositForm != null ? depositForm.getIdBackFileId() : null)
                .idBackFileUrl(fileDownloadUrl(depositForm != null ? depositForm.getIdBackFileId() : null))
                .portraitFileId(depositForm != null ? depositForm.getPortraitFileId() : null)
                .portraitFileUrl(fileDownloadUrl(depositForm != null ? depositForm.getPortraitFileId() : null))
                .note(depositAgreement.getNote())
                .createdAt(depositAgreement.getCreatedAt())
                .build();
    }

    private DepositForm getDepositForm(DepositAgreement depositAgreement) {
        if (depositAgreement.getDepositFormId() == null) {
            return null;
        }
        return depositFormRepository.findById(depositAgreement.getDepositFormId()).orElse(null);
    }

    private String fileDownloadUrl(Long fileId) {
        return fileId == null ? null : "/api/v1/tenants/profiles/me/files/" + fileId;
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

    private void validateManagementInfoUpdate(DepositAgreementManagementUpdateRequest request) {
        String normalizedPhone = normalizePhone(request.depositorPhone());
        if (!normalizedPhone.matches("^0\\d{9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số.");
        }
        if (request.permanentAddress() == null || request.permanentAddress().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ không được để trống.");
        }

        LocalDate today = LocalDate.now();
        if (request.expectedLeaseSignDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày ký hợp đồng dự kiến không được là ngày quá khứ.");
        }
        if (request.expectedMoveInDate().isBefore(today)) {
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
        if (nextStatus == DepositAgreementStatus.REFUNDED || nextStatus == DepositAgreementStatus.FORFEITED) {
            room.releaseRoom();
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

    private void assertCanAccessContract(DepositAgreement depositAgreement) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem hợp đồng đặt cọc.");
        }

        Role role = principal.getRole();
        if (role == Role.OWNER || role == Role.MANAGER) {
            return;
        }

        if (role == Role.TENANT && depositAgreement.getTenantId() != null) {
            var tenant = tenantRepository.findByUserId(principal.getId()).orElse(null);
            if (tenant != null && tenant.getId().equals(depositAgreement.getTenantId())) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hợp đồng đặt cọc này.");
    }
}
