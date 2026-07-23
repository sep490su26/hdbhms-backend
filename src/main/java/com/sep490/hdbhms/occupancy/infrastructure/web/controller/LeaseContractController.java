package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestResponse;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.service.ContractLifecycleChangeRequestService;
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
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lease-contracts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractController {
    private static final DateTimeFormatter DOCUMENT_FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    GetRoomDetailsUseCase getRoomDetailsUseCase;
    LeaseContractWebMapper leaseContractWebMapper;
    GetMyListLeaseContractsUseCase getMyListLeaseContractsUseCase;
    GetLeaseContractDetailsUseCase getLeaseContractDetailsUseCase;
    LeaseContractManagementService leaseContractManagementService;
    ContractLifecycleChangeRequestService contractLifecycleChangeRequestService;
    LeaseContractQueryService leaseContractQueryService;
    LeaseContractDocumentService leaseContractDocumentService;
    DownloadFileUseCase downloadFileUseCase;
    RoomCommitmentChecker roomCommitmentChecker;
    JdbcTemplate jdbcTemplate;

    @GetMapping("/{id}/draft-pdf")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getDraftPdf(@PathVariable Long id) {
        assertOwnerOrAssignedManagerCanAccessContract(id);
        LeaseContractManagementResponse contract = leaseContractManagementService.findOne(id);
        byte[] pdfBytes = leaseContractDocumentService.generateDraftPdf(id);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        String filename = leaseContractFilename(contract);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<PageResponse<LeaseContractManagementResponse>> getManagementContracts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<LeaseContractManagementResponse>>builder()
                .data(leaseContractManagementService.findAllForManagement(pageable))
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
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFileForDeposit(
            @PathVariable Long depositAgreementId,
            @RequestPart("file") MultipartFile file
    ) {
        assertOwnerOrAssignedManagerCanAccessDeposit(depositAgreementId);
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFileForDeposit(depositAgreementId, file))
                .build();
    }

    @PostMapping("/management/deposits/{depositAgreementId}/draft")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> createDraftLeaseContractForDeposit(
            @PathVariable Long depositAgreementId
    ) {
        assertOwnerOrAssignedManagerCanAccessDeposit(depositAgreementId);
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.createDraftLeaseContractForDeposit(depositAgreementId))
                .build();
    }

    @PostMapping("/{leaseContractId}/signed-file")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> uploadSignedFile(
            @PathVariable Long leaseContractId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean replace
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(leaseContractId);
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(leaseContractManagementService.uploadSignedFile(leaseContractId, file, replace))
                .build();
    }

    @GetMapping("/{leaseContractId}/signed-file")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadSignedLeaseContractFile(
            @PathVariable Long leaseContractId
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(leaseContractId);
        LeaseContractManagementResponse contract = leaseContractManagementService.findOne(leaseContractId);
        if (contract.getSignedFileId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chua co ban hop dong thue da ky.");
        }

        FileDataResponse fileData = downloadFileUseCase.execute(new DownloadFileQuery(contract.getSignedFileId()));
        if (fileData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay file hop dong thue da ky.");
        }
        String contentType = fileData.contentType() == null
                ? org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
                : fileData.contentType();
        String filename = leaseContractFilename(contract);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .body(fileData.resource());
    }

    @PostMapping("/{leaseContractId}/activate")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> activateLeaseContract(
            @PathVariable Long leaseContractId
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(leaseContractId);
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
                        request.newContractCode(),
                        request.note()
                ))
                .build();
    }

    @PostMapping("/{leaseContractId}/liquidation-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<ChangeRequestResponse> submitLiquidationRequest(
            @PathVariable Long leaseContractId,
            @RequestBody(required = false) LeaseContractLiquidationRequest request
    ) {
        leaseContractQueryService.assertCurrentUserCanReadContract(leaseContractId);
        ChangeRequest changeRequest = contractLifecycleChangeRequestService.submitLiquidationRequest(
                leaseContractId,
                request == null ? null : request.liquidationDate(),
                request == null ? null : request.reason()
        );
        return ApiResponse.<ChangeRequestResponse>builder()
                .data(toChangeRequestResponse(changeRequest))
                .build();
    }

    @PostMapping("/{leaseContractId}/renewal-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<ChangeRequestResponse> submitRenewalRequest(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody LeaseContractRenewalRequest request
    ) {
        leaseContractQueryService.assertCurrentUserCanReadContract(leaseContractId);
        ChangeRequest changeRequest = contractLifecycleChangeRequestService.submitRenewalRequest(
                leaseContractId,
                request.newStartDate(),
                request.newEndDate(),
                request.monthlyRent(),
                request.paymentCycleMonths(),
                request.depositAmount(),
                request.note()
        );
        return ApiResponse.<ChangeRequestResponse>builder()
                .data(toChangeRequestResponse(changeRequest))
                .build();
    }

    @PostMapping("/{leaseContractId}/co-occupant-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<ChangeRequestResponse> submitAddCoOccupantRequest(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody AddCoOccupantRequest request
    ) {
        leaseContractQueryService.assertCurrentUserCanReadContract(leaseContractId);
        ChangeRequest changeRequest = contractLifecycleChangeRequestService.submitAddCoOccupantRequest(
                leaseContractId,
                request.tenantProfileId(),
                request.fullName(),
                request.dob(),
                request.gender(),
                request.phone(),
                request.email(),
                request.permanentAddress(),
                request.moveInDate(),
                request.note()
        );
        return ApiResponse.<ChangeRequestResponse>builder()
                .data(toChangeRequestResponse(changeRequest))
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
                            fm.original_name AS contract_file_name,
                            lc.signed_file_id,
                            sfm.original_name AS signed_file_name
                        FROM lease_contracts lc
                        LEFT JOIN file_metadata fm ON fm.file_metadata_id = lc.contract_file_id
                        LEFT JOIN file_metadata sfm ON sfm.file_metadata_id = lc.signed_file_id
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
                    Long signedFileId = rs.getObject("signed_file_id", Long.class);
                    response.setSignedFileId(signedFileId);
                    response.setSignedFileName(rs.getString("signed_file_name"));
                    response.setSignedFileUrl(signedFileId == null ? null : "/api/v1/lease-contracts/" + leaseContractId + "/signed-file");
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

    private String leaseContractFilename(LeaseContractManagementResponse contract) {
        String roomCode = withRoomPrefix(sanitizeFilenamePart(contract.getRoomCode(), "Phong-X"));
        String date = contract.getStartDate() == null
                ? "Chua-Ro-Ngay"
                : DOCUMENT_FILENAME_DATE_FORMATTER.format(contract.getStartDate());
        return "HDT_" + roomCode + "_" + date + ".pdf";
    }

    private String sanitizeFilenamePart(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private String withRoomPrefix(String roomCode) {
        if (roomCode.startsWith("Phong")) {
            return roomCode;
        }
        if (roomCode.regionMatches(true, 0, "P", 0, 1)) {
            return "P" + roomCode.substring(1);
        }
        return "P" + roomCode;
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

    private void assertOwnerOrAssignedManagerCanAccessContract(Long leaseContractId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap.");
        }
        if (principal.getRole() == Role.OWNER) {
            return;
        }
        if (principal.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thao tac hop dong nay.");
        }

        Long propertyId = jdbcTemplate.query("""
                        SELECT r.property_id
                        FROM lease_contracts lc
                        JOIN rooms r ON r.room_id = lc.room_id
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("property_id") : null,
                leaseContractId
        );
        if (propertyId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong thue.");
        }

        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM role_promotions
                        WHERE user_id = ?
                          AND property_id = ?
                          AND role = 'MANAGER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                principal.getId(),
                propertyId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thao tac hop dong nay.");
        }
    }

    private void assertOwnerOrAssignedManagerCanAccessDeposit(Long depositAgreementId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap.");
        }
        if (principal.getRole() == Role.OWNER) {
            return;
        }
        if (principal.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thao tac hop dong coc nay.");
        }

        Long propertyId = jdbcTemplate.query("""
                        SELECT r.property_id
                        FROM deposit_agreements da
                        JOIN rooms r ON r.room_id = da.room_id
                        WHERE da.deposit_agreement_id = ?
                          AND r.deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("property_id") : null,
                depositAgreementId
        );
        if (propertyId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay hop dong dat coc.");
        }

        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM role_promotions
                        WHERE user_id = ?
                          AND property_id = ?
                          AND role = 'MANAGER'
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                principal.getId(),
                propertyId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong co quyen thao tac hop dong coc nay.");
        }
    }

    public record LeaseContractLiquidationRequest(
            LocalDate liquidationDate,
            String reason
    ) {
    }

    public record AddCoOccupantRequest(
            Long tenantProfileId,
            @Size(max = 255, message = "Ten nguoi o cung khong duoc vuot qua 255 ky tu.")
            String fullName,
            LocalDate dob,
            Gender gender,
            @Size(max = 30, message = "So dien thoai khong duoc vuot qua 30 ky tu.")
            String phone,
            @Email(message = "Email nguoi o cung khong hop le.")
            @Size(max = 255, message = "Email nguoi o cung khong duoc vuot qua 255 ky tu.")
            String email,
            @Size(max = 1000, message = "Dia chi thuong tru khong duoc vuot qua 1000 ky tu.")
            String permanentAddress,
            LocalDate moveInDate,
            @Size(max = 1000, message = "Ghi chu khong duoc vuot qua 1000 ky tu.")
            String note
    ) {
    }

    private ChangeRequestResponse toChangeRequestResponse(ChangeRequest req) {
        return new ChangeRequestResponse(
                req.getId(),
                req.getRequestCode(),
                req.getRequestType(),
                req.getTargetType(),
                req.getTargetId(),
                req.getTitle(),
                req.getDescription(),
                req.getRequestPayload(),
                req.getStatus(),
                req.getRequesterId(),
                req.getResolutionNote(),
                req.getResolvedAt(),
                req.getCreatedAt()
        );
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
