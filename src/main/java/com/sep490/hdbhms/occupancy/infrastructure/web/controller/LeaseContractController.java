package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractDocumentService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractManagementService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomRentalHistoryResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.LeaseContractWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lease-contracts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractController {
    GetRoomDetailsUseCase getRoomDetailsUseCase;
    LeaseContractWebMapper leaseContractWebMapper;
    GetMyListLeaseContractsUseCase getMyListLeaseContractsUseCase;
    GetLeaseContractDetailsUseCase getLeaseContractDetailsUseCase;
    LeaseContractManagementService leaseContractManagementService;
    LeaseContractQueryService leaseContractQueryService;
    LeaseContractDocumentService leaseContractDocumentService;
    RoomCommitmentChecker roomCommitmentChecker;
    JdbcTemplate jdbcTemplate;

    @GetMapping("/{id}/draft-pdf")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getDraftPdf(@PathVariable Long id) {
        byte[] pdfBytes = leaseContractDocumentService.generateDraftPdf(id);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"draft-contract.pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<List<LeaseContractManagementResponse>> getManagementContracts() {
        return ApiResponse.<List<LeaseContractManagementResponse>>builder()
                .data(leaseContractManagementService.findAllForManagement())
                .build();
    }

    @GetMapping("/management/{leaseContractId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractQueryDetailsResponse> getManagementContractDetails(
            @PathVariable Long leaseContractId
    ) {
        return ApiResponse.<LeaseContractQueryDetailsResponse>builder()
                .data(leaseContractQueryService.getManagementContractDetails(leaseContractId))
                .build();
    }

    @GetMapping("/management/rooms/{roomId}/rental-history")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<RoomRentalHistoryResponse> getManagementRoomRentalHistory(
            @PathVariable Long roomId
    ) {
        return ApiResponse.<RoomRentalHistoryResponse>builder()
                .data(leaseContractQueryService.getManagementRoomRentalHistory(roomId))
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFileForDeposit(
            @PathVariable Long depositAgreementId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFileForDeposit(depositAgreementId, file))
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/draft")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> createDraftLeaseContractForDeposit(
            @PathVariable Long depositAgreementId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.createDraftLeaseContractForDeposit(depositAgreementId))
                .build();
    }

    @PostMapping("/{leaseContractId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFile(
            @PathVariable Long leaseContractId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFile(leaseContractId, file))
                .build();
    }

    @PostMapping("/{leaseContractId}/activate")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> activateLeaseContract(
            @PathVariable Long leaseContractId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.activate(leaseContractId))
                .build();
    }

    @PatchMapping("/{leaseContractId}/terms")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> updateLeaseContractTerms(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody LeaseContractTermsUpdateRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.updateTerms(
                        leaseContractId,
                        request.startDate(),
                        request.endDate(),
                        request.paymentCycleMonths(),
                        request.monthlyRent(),
                        request.depositAmount()
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/liquidate")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> liquidateLeaseContract(
            @PathVariable Long leaseContractId,
            @RequestBody(required = false) LeaseContractLiquidationRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.liquidate(
                        leaseContractId,
                        request != null ? request.liquidationDate() : null,
                        request != null ? request.reason() : null
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractRenewalResponse> renewLeaseContract(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody LeaseContractRenewalRequest request
    ) {
        return ApiResponse.<LeaseContractRenewalResponse>builder()
                .data(leaseContractManagementService.renew(
                        leaseContractId,
                        request.newStartDate(),
                        request.newEndDate(),
                        request.monthlyRent(),
                        request.paymentCycleMonths(),
                        request.depositAmount(),
                        request.note()
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/tenant-intention")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<LeaseContractManagementResponse> recordTenantIntention(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody TenantIntentionRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.recordTenantIntentionForCurrentUser(
                        leaseContractId,
                        request.intention(),
                        request.expectedMoveOutDate(),
                        request.note()
                ))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<LeaseContractResponse>> getMyLeaseContracts(
            @RequestParam(required = false) LeaseStatus status,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<PageResponse<LeaseContractResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getMyListLeaseContractsUseCase.execute(
                                        new GetListLeaseContractsQuery(
                                                userId,
                                                status,
                                                signedFrom,
                                                signedTo,
                                                pageable
                                        )
                                ).map(leaseContract -> {
                                    Room room = getRoomDetailsUseCase.execute(
                                            new GetRoomDetailsQuery(leaseContract.getRoomId())
                                    );
                                    return LeaseContractResponse.builder()
                                            .id(leaseContract.getId())
                                            .contractCode(leaseContract.getContractCode())
                                            .roomId(room.getId())
                                            .roomCode(room.getRoomCode())
                                            .roomName(room.getName())
                                            .status(leaseContract.getStatus())
                                            .signedAt(leaseContract.getSignedAt())
                                            .build();
                                })
                        )
                )
                .build();
    }

    @GetMapping("/me/active-rooms")
    public ApiResponse<List<LeaseContractQueryService.ActiveRoomItem>> getMyActiveRooms() {
        return ApiResponse.<List<LeaseContractQueryService.ActiveRoomItem>>builder()
                .data(leaseContractQueryService.getMyActiveRooms())
                .build();
    }

    @GetMapping("/me/rental-contexts")
    public ApiResponse<List<LeaseContractQueryService.ActiveRoomItem>> getMyRentalContexts() {
        return ApiResponse.<List<LeaseContractQueryService.ActiveRoomItem>>builder()
                .data(leaseContractQueryService.getMyActiveRooms())
                .build();
    }

    @GetMapping("/{leaseContractId}")
    public ApiResponse<LeaseContractDetailsResponse> getLeaseContractDetails(
            @PathVariable("leaseContractId") Long leaseContractId
    ) {
        leaseContractQueryService.assertCurrentUserCanReadContract(leaseContractId);
        LeaseContract leaseContract = getLeaseContractDetailsUseCase.execute(
                new GetLeaseContractDetailsQuery(leaseContractId)
        );
        Room room = getRoomDetailsUseCase.execute(
                new GetRoomDetailsQuery(leaseContract.getRoomId())
        );
        LeaseContractDetailsResponse response = leaseContractWebMapper.toDetailsResponse(
                leaseContract,
                room
        );
        enrichTenantContractDetails(response, leaseContractId);
        return ApiResponse.<LeaseContractDetailsResponse>builder()
                .data(response)
                .build();
    }

    private void enrichTenantContractDetails(LeaseContractDetailsResponse response, Long leaseContractId) {
        jdbcTemplate.query("""
                        SELECT
                            lc.tenant_intention,
                            lc.expected_vacant_date,
                            lc.contract_file_id,
                            fm.original_name AS contract_file_name
                        FROM lease_contracts lc
                        LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                        WHERE lc.lease_contract_id = ?
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Long fileId = rs.getObject("contract_file_id", Long.class);
                    response.setTenantIntention(rs.getString("tenant_intention"));
                    response.setExpectedVacantDate(rs.getObject("expected_vacant_date", LocalDate.class));
                    response.setContractFileId(fileId);
                    response.setContractFileName(rs.getString("contract_file_name"));
                    response.setContractFileUrl(fileId == null ? null : "/api/v1/tenants/profiles/me/files/" + fileId);
                    return null;
                },
                leaseContractId
        );

        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            return;
        }
        boolean isPrimary = isCurrentUserPrimarySigner(leaseContractId, userId);
        boolean isOccupant = isPrimary || isCurrentUserActiveOccupant(leaseContractId, userId);
        response.setIsPrimary(isPrimary);
        response.setRoleInContract(isPrimary ? "PRIMARY" : isOccupant ? "CO_OCCUPANT" : null);
        boolean canRecordIntention = isPrimary
                && List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(response.getStatus())
                && response.getEndDate() != null
                && !LocalDate.now().isBefore(response.getEndDate().minusMonths(3));
        response.setCanRecordIntention(canRecordIntention);

        RoomCommitmentChecker.Blocker renewBlocker = response.getRoom() == null || response.getRoom().getId() == null
                ? RoomCommitmentChecker.Blocker.NONE
                : roomCommitmentChecker.checkRenewBlockers(response.getRoom().getId(), leaseContractId);
        response.setCanRenew(canRecordIntention && renewBlocker == RoomCommitmentChecker.Blocker.NONE);
        response.setCanRenewBlockedReason(renewBlocker == RoomCommitmentChecker.Blocker.NONE
                ? null
                : renewBlockedMessage(renewBlocker));
    }

    private String renewBlockedMessage(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_HOLD_IN_PROGRESS) {
            return "Phòng đang được giữ chỗ cho khách khác. Vui lòng liên hệ quản lý.";
        }
        return "Phòng đã có khách khác đặt cọc/giữ chỗ, không thể gia hạn. Vui lòng liên hệ quản lý.";
    }

    private boolean isCurrentUserPrimarySigner(Long leaseContractId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM lease_contracts lc
                        JOIN person_profiles pp ON pp.person_profile_id = lc.primary_tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.person_profile_id
                              AND tap.user_id = ?
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR tap.user_id = ?)
                        """,
                Integer.class,
                userId,
                leaseContractId,
                userId,
                userId
        );
        return count != null && count > 0;
    }

    private boolean isCurrentUserActiveOccupant(Long leaseContractId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM contract_occupants co
                        JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.person_profile_id
                              AND tap.user_id = ?
                        WHERE co.contract_id = ?
                          AND co.status = 'ACTIVE'
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR tap.user_id = ?)
                        """,
                Integer.class,
                userId,
                leaseContractId,
                userId,
                userId
        );
        return count != null && count > 0;
    }

    public record LeaseContractLiquidationRequest(
            LocalDate liquidationDate,
            String reason
    ) {
    }

    public record LeaseContractTermsUpdateRequest(
            @NotNull(message = "Ngày bắt đầu hợp đồng là bắt buộc.")
            LocalDate startDate,
            @NotNull(message = "Ngày kết thúc hợp đồng là bắt buộc.")
            LocalDate endDate,
            @NotNull(message = "Chu kỳ thanh toán là bắt buộc.")
            Integer paymentCycleMonths,
            @NotNull(message = "Giá thuê mỗi tháng là bắt buộc.")
            @Positive(message = "Giá thuê mỗi tháng phải lớn hơn 0.")
            Long monthlyRent,
            @NotNull(message = "Tiền cọc là bắt buộc.")
            @PositiveOrZero(message = "Tiền cọc phải lớn hơn hoặc bằng 0.")
            Long depositAmount
    ) {
    }

    public record LeaseContractRenewalRequest(
            @Size(max = 80, message = "Mã hợp đồng mới không được vượt quá 80 ký tự.")
            String newContractCode,
            @NotNull(message = "Ngày bắt đầu mới là bắt buộc.")
            LocalDate newStartDate,
            @NotNull(message = "Ngày kết thúc mới là bắt buộc.")
            LocalDate newEndDate,
            @NotNull(message = "Giá thuê mỗi tháng là bắt buộc.")
            @Positive(message = "Giá thuê mỗi tháng phải lớn hơn 0.")
            Long monthlyRent,
            @NotNull(message = "Chu kỳ thanh toán là bắt buộc.")
            Integer paymentCycleMonths,
            @NotNull(message = "Tiền cọc là bắt buộc.")
            @PositiveOrZero(message = "Tiền cọc phải lớn hơn hoặc bằng 0.")
            Long depositAmount,
            @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }

    public record TenantIntentionRequest(
            @NotNull(message = "Ý định khách là bắt buộc.")
            String intention,
            LocalDate expectedMoveOutDate,
            @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }
}
