package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestResponse;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.TenantAccountProvisioningStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.application.port.in.command.LeaseContractLiquidationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.LiquidationChargeInput;
import com.sep490.hdbhms.occupancy.application.port.in.command.RecordTenantIntentionCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.RenewLeaseContractCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ActivateLeaseContractRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateLeaseContractActivationReadingRequest;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.property.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ActivateLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateDraftLeaseContractForDepositUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CompleteLeaseLiquidationUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyListLeaseContractsUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.GetRoomDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RecordTenantIntentionUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RenewLeaseContractUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractTermsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseContractActivationReadingUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseLiquidationDraftUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UploadSignedLeaseContractFileUseCase;
import com.sep490.hdbhms.occupancy.application.service.ContractLifecycleChangeRequestService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractDocumentService;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractDebtPolicy;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantRole;
import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractQueryDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomRentalHistoryResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.LeaseContractWebMapper;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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

import java.math.BigDecimal;
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
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;
    UploadSignedLeaseContractFileUseCase uploadSignedLeaseContractFileUseCase;
    ActivateLeaseContractUseCase activateLeaseContractUseCase;
    CreateDraftLeaseContractForDepositUseCase createDraftLeaseContractForDepositUseCase;
    UpdateLeaseContractTermsUseCase updateLeaseContractTermsUseCase;
    UpdateLeaseContractActivationReadingUseCase updateLeaseContractActivationReadingUseCase;
    CompleteLeaseLiquidationUseCase completeLeaseLiquidationUseCase;
    UpdateLeaseLiquidationDraftUseCase updateLeaseLiquidationDraftUseCase;
    RenewLeaseContractUseCase renewLeaseContractUseCase;
    RecordTenantIntentionUseCase recordTenantIntentionUseCase;
    ContractLifecycleChangeRequestService contractLifecycleChangeRequestService;
    LeaseContractQueryService leaseContractQueryService;
    LeaseContractDocumentService leaseContractDocumentService;
    DownloadFileUseCase downloadFileUseCase;
    RoomCommitmentChecker roomCommitmentChecker;
    PersonProfileRepository personProfileRepository;
    JdbcTemplate jdbcTemplate;

    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getDraftPdf(Long id) {
        return getDraftPdf(id, null);
    }

    @GetMapping("/{id}/draft-pdf")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getDraftPdf(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal electricityValue
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(id);
        LeaseContractManagementResponse contract = getLeaseContractManagementUseCase.findOne(id);
        byte[] pdfBytes = electricityValue == null
                ? leaseContractDocumentService.generateDraftPdf(id)
                : leaseContractDocumentService.generateDraftPdf(id, electricityValue);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        String filename = leaseContractFilename(contract);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PostMapping("/management/deposits/{depositFormId}/draft")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> createDraftFromDeposit(
            @PathVariable Long depositFormId
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(createDraftLeaseContractForDepositUseCase.execute(depositFormId))
                .build();
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<PageResponse<LeaseContractManagementResponse>> getManagementContracts(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<LeaseContractManagementResponse>>builder()
                .data(getLeaseContractManagementUseCase.findAll(pageable))
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
                .data(uploadSignedLeaseContractFileUseCase.execute(leaseContractId, file, replace))
                .build();
    }

    @GetMapping("/{leaseContractId}/signed-file")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadSignedLeaseContractFile(
            @PathVariable Long leaseContractId
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(leaseContractId);
        LeaseContractManagementResponse contract = getLeaseContractManagementUseCase.findOne(leaseContractId);
        if (contract.getSignedFileId() == null) {
            throw new AppException(ApiErrorCode.MIGRATED_CHUA_CO_BAN_HOP_DONG_THUE_DA_KY);
        }

        FileDataResponse fileData = downloadFileUseCase.execute(new DownloadFileQuery(contract.getSignedFileId()));
        if (fileData == null) {
            throw new AppException(ApiErrorCode.MIGRATED_KHONG_TIM_THAY_FILE_HOP_DONG_THUE_DA_KY);
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
            @PathVariable Long leaseContractId,
            @Valid @RequestBody(required = false) ActivateLeaseContractRequest request
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(leaseContractId);
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(activateLeaseContractUseCase.execute(leaseContractId, request))
                .build();
    }

    @PatchMapping("/{leaseContractId}/activation-reading")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> updateActivationReading(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody UpdateLeaseContractActivationReadingRequest request
    ) {
        assertOwnerOrAssignedManagerCanAccessContract(leaseContractId);
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(updateLeaseContractActivationReadingUseCase.execute(leaseContractId, request))
                .build();
    }

    @PatchMapping("/{leaseContractId}/terms")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<LeaseContractManagementResponse> updateLeaseContractTerms(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody LeaseContractTermsUpdateRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(updateLeaseContractTermsUseCase.execute(new UpdateLeaseContractTermsCommand(
                        leaseContractId,
                        request.startDate(),
                        request.endDate(),
                        request.paymentCycleMonths(),
                        request.monthlyRent(),
                        request.depositAmount()
                )))
                .build();
    }

    @PostMapping("/{leaseContractId}/liquidate")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> liquidateLeaseContract(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody(required = false) LeaseContractLiquidationRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(completeLeaseLiquidationUseCase.execute(new LeaseContractLiquidationCommand(
                        leaseContractId,
                        request != null ? request.liquidationDate() : null,
                        request != null ? request.reason() : null,
                        liquidationCharges(request)
                )))
                .build();
    }

    @PatchMapping("/{leaseContractId}/liquidation")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LeaseContractManagementResponse> updateLiquidationDraft(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody(required = false) LeaseContractLiquidationRequest request
    ) {
        return ApiResponse.<LeaseContractManagementResponse>builder()
                .data(updateLeaseLiquidationDraftUseCase.execute(new LeaseContractLiquidationCommand(
                        leaseContractId,
                        request != null ? request.liquidationDate() : null,
                        request != null ? request.reason() : null,
                        liquidationCharges(request)
                )))
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
                .data(renewLeaseContractUseCase.execute(new RenewLeaseContractCommand(
                        leaseContractId,
                        request.newStartDate(),
                        request.newEndDate(),
                        request.monthlyRent(),
                        request.paymentCycleMonths(),
                        request.depositAmount(),
                        request.newContractCode(),
                        request.note()
                )))
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
                request == null ? null : request.reason(),
                request == null ? null : request.liquidationMode(),
                request == null ? null : request.leavingProfileIds(),
                request == null ? null : request.stayingProfileIds(),
                request == null ? null : request.replacementPrimaryTenantProfileId()
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
                request.renewalTermMonths(),
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
                .data(recordTenantIntentionUseCase.executeForCurrentUser(new RecordTenantIntentionCommand(
                        leaseContractId,
                        request.intention(),
                        request.expectedMoveOutDate(),
                        request.note()
                )))
                .build();
    }

    @PostMapping("/{leaseContractId}/occupant-intention")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<LeaseContractDetailsResponse> recordOccupantIntention(
            @PathVariable Long leaseContractId,
            @Valid @RequestBody OccupantIntentionRequest request
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        OccupantScope occupant = currentActiveOccupant(leaseContractId, userId);
        jdbcTemplate.update("""
                        INSERT INTO contract_occupant_intentions (
                            contract_id,
                            contract_occupant_id,
                            tenant_profile_id,
                            intention,
                            note,
                            recorded_at
                        )
                        VALUES (?, ?, ?, ?, ?, NOW(6))
                        ON DUPLICATE KEY UPDATE
                            tenant_profile_id = VALUES(tenant_profile_id),
                            intention = VALUES(intention),
                            note = VALUES(note),
                            recorded_at = NOW(6)
                        """,
                leaseContractId,
                occupant.contractOccupantId(),
                occupant.tenantProfileId(),
                normalizeOccupantIntention(request.intention()),
                blankToNull(request.note())
        );
        return getLeaseContractDetails(leaseContractId);
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
        response.setOccupants(findTenantContractOccupants(leaseContractId));
        response.setCurrentTenantProfileId(findCurrentTenantProfileId(leaseContractId, userId));
        boolean isPrimary = isCurrentUserPrimarySigner(leaseContractId, userId);
        boolean isOccupant = isPrimary || isCurrentUserActiveOccupant(leaseContractId, userId);
        response.setIsPrimary(isPrimary);
        response.setRoleInContract(isPrimary ? "PRIMARY" : isOccupant ? "CO_OCCUPANT" : null);
        response.setCanRecordOccupantIntention(false);
        boolean canRecordIntention = isPrimary
                && List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON).contains(response.getStatus())
                && response.getEndDate() != null
                && !LocalDate.now().isBefore(response.getEndDate().minusMonths(3));
        response.setCanRecordIntention(canRecordIntention);
        if (!isPrimary && isOccupant) {
            response.setCanRecordOccupantIntention(
                    List.of(LeaseStatus.ACTIVE, LeaseStatus.EXPIRING_SOON, LeaseStatus.TERMINATION_PENDING)
                            .contains(response.getStatus())
            );
            enrichCurrentOccupantIntention(response, leaseContractId, userId);
        }

        RoomCommitmentChecker.Blocker renewBlocker = response.getRoom() == null || response.getRoom().getId() == null
                ? RoomCommitmentChecker.Blocker.NONE
                : roomCommitmentChecker.checkRenewBlockers(
                        response.getRoom().getId(),
                        leaseContractId,
                        response.getEndDate()
                );
        long outstandingDebt = LeaseContractDebtPolicy.outstandingAmount(jdbcTemplate, leaseContractId);
        String debtBlockedReason = LeaseContractDebtPolicy.blockingReason(outstandingDebt);
        response.setCanRenew(canRecordIntention
                && renewBlocker == RoomCommitmentChecker.Blocker.NONE
                && outstandingDebt == 0);
        response.setCanRenewBlockedReason(debtBlockedReason != null
                ? debtBlockedReason
                : renewBlocker == RoomCommitmentChecker.Blocker.NONE
                ? null
                : renewBlockedMessage(renewBlocker));
        boolean liquidationBlockedByBooking = response.getRoom() != null
                && response.getRoom().getId() != null
                && roomCommitmentChecker.isSoonVacantBookingCase(
                response.getRoom().getId(),
                leaseContractId,
                response.getEndDate()
        );
        response.setCanLiquidate(List.of(
                LeaseStatus.ACTIVE,
                LeaseStatus.EXPIRING_SOON,
                LeaseStatus.EXPIRED,
                LeaseStatus.TERMINATION_PENDING
        ).contains(response.getStatus()) && !liquidationBlockedByBooking && outstandingDebt == 0);
        response.setCanLiquidateBlockedReason(debtBlockedReason != null
                ? debtBlockedReason
                : liquidationBlockedByBooking
                ? ApiErrorCode.ROOM_LIQUIDATION_BLOCKED_BY_BOOKING.getDetails()
                : null);
    }

    private List<LeaseContractQueryDetailsResponse.OccupantInfo> findTenantContractOccupants(Long leaseContractId) {
        return jdbcTemplate.query("""
                        SELECT
                            pp.person_profile_id AS tenant_profile_id,
                            pp.full_name,
                            pp.phone,
                            COALESCE(pp.email, u.email) AS email,
                            co.occupant_role,
                            co.move_in_date,
                            co.move_out_date,
                            co.status
                        FROM (
                            SELECT
                                active_occupant.contract_occupant_id AS id,
                                active_occupant.contract_id,
                                active_occupant.tenant_profile_id,
                                active_occupant.occupant_role,
                                active_occupant.move_in_date,
                                active_occupant.move_out_date,
                                active_occupant.status
                            FROM contract_occupants active_occupant
                            WHERE active_occupant.status = 'ACTIVE'
                              AND active_occupant.tenant_profile_id IS NOT NULL

                            UNION ALL

                            SELECT
                                NULL AS id,
                                fallback_contract.lease_contract_id AS contract_id,
                                fallback_contract.primary_tenant_profile_id AS tenant_profile_id,
                                'PRIMARY' AS occupant_role,
                                fallback_contract.start_date AS move_in_date,
                                NULL AS move_out_date,
                                'ACTIVE' AS status
                            FROM lease_contracts fallback_contract
                            WHERE fallback_contract.deleted_at IS NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM contract_occupants primary_occupant
                                  WHERE primary_occupant.contract_id = fallback_contract.lease_contract_id
                                    AND primary_occupant.tenant_profile_id = fallback_contract.primary_tenant_profile_id
                              )
                        ) co
                        JOIN person_profiles pp
                          ON pp.person_profile_id = co.tenant_profile_id
                         AND pp.deleted_at IS NULL
                        LEFT JOIN users u
                          ON u.user_id = pp.user_id
                         AND u.deleted_at IS NULL
                        WHERE co.contract_id = ?
                        ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.id
                        """,
                (rs, rowNum) -> new LeaseContractQueryDetailsResponse.OccupantInfo(
                        rs.getLong("tenant_profile_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        OccupantRole.valueOf(rs.getString("occupant_role")),
                        rs.getObject("move_in_date", LocalDate.class),
                        rs.getObject("move_out_date", LocalDate.class),
                        OccupantStatus.valueOf(rs.getString("status")),
                        TenantAccountProvisioningStatus.NOT_PROVISIONED,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                leaseContractId
        );
    }

    private Long findCurrentTenantProfileId(Long leaseContractId, Long userId) {
        return jdbcTemplate.query("""
                        SELECT pp.person_profile_id
                        FROM lease_contracts lc
                        JOIN person_profiles pp
                          ON pp.deleted_at IS NULL
                         AND (
                              pp.person_profile_id = lc.primary_tenant_profile_id
                              OR EXISTS (
                                  SELECT 1
                                  FROM contract_occupants co
                                  WHERE co.contract_id = lc.lease_contract_id
                                    AND co.tenant_profile_id = pp.person_profile_id
                                    AND co.status = 'ACTIVE'
                              )
                         )
                        LEFT JOIN tenant_account_provisionings tap
                          ON tap.tenant_profile_id = pp.person_profile_id
                         AND tap.user_id = ?
                         AND tap.status <> 'DISABLED'
                        WHERE lc.lease_contract_id = ?
                          AND lc.deleted_at IS NULL
                          AND (pp.user_id = ? OR tap.user_id = ?)
                        ORDER BY CASE WHEN pp.person_profile_id = lc.primary_tenant_profile_id THEN 0 ELSE 1 END
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("person_profile_id") : null,
                userId,
                leaseContractId,
                userId,
                userId
        );
    }

    private String renewBlockedMessage(RoomCommitmentChecker.Blocker blocker) {
        if (blocker == RoomCommitmentChecker.Blocker.ROOM_ALREADY_RESERVED_BY_NEW_TENANT) {
            return "Phòng đang được giữ chỗ cho khách khác. Vui lòng liên hệ quản lý.";
        }
        return "Phòng đã có khách khác đặt cọc/giữ chỗ, không thể gia hạn. Vui lòng liên hệ quản lý.";
    }

    private String leaseContractFilename(LeaseContractManagementResponse contract) {
        return DocumentFilenameBuilder.buildLeaseContractCode(
                contract.getRoomCode(),
                contract.getStartDate()
        ) + ".pdf";
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

    private OccupantScope currentActiveOccupant(Long leaseContractId, Long userId) {
        if (userId == null) {
            throw new AppException(ApiErrorCode.MIGRATED_CHUA_DANG_NHAP);
        }
        return jdbcTemplate.query("""
                        SELECT co.contract_occupant_id, co.tenant_profile_id
                        FROM contract_occupants co
                        JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.person_profile_id
                              AND tap.user_id = ?
                        WHERE co.contract_id = ?
                          AND co.status = 'ACTIVE'
                          AND co.occupant_role = 'CO_OCCUPANT'
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR tap.user_id = ?)
                        ORDER BY co.contract_occupant_id DESC
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new AppException(ApiErrorCode.MIGRATED_BAN_KHONG_PHAI_NGUOI_O_CUNG_CUA_HOP_DONG_NAY);
                    }
                    return new OccupantScope(
                            rs.getLong("contract_occupant_id"),
                            rs.getLong("tenant_profile_id")
                    );
                },
                userId,
                leaseContractId,
                userId,
                userId
        );
    }

    private void enrichCurrentOccupantIntention(LeaseContractDetailsResponse response, Long leaseContractId, Long userId) {
        jdbcTemplate.query("""
                        SELECT coi.intention, coi.note, coi.recorded_at
                        FROM contract_occupant_intentions coi
                        JOIN contract_occupants co
                          ON co.contract_occupant_id = coi.contract_occupant_id
                        JOIN person_profiles pp
                          ON pp.person_profile_id = coi.tenant_profile_id
                        LEFT JOIN tenant_account_provisionings tap
                               ON tap.tenant_profile_id = pp.person_profile_id
                              AND tap.user_id = ?
                        WHERE coi.contract_id = ?
                          AND co.status = 'ACTIVE'
                          AND pp.deleted_at IS NULL
                          AND (pp.user_id = ? OR tap.user_id = ?)
                        ORDER BY coi.recorded_at DESC
                        LIMIT 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    response.setOccupantIntention(rs.getString("intention"));
                    response.setOccupantIntentionNote(rs.getString("note"));
                    response.setOccupantIntentionRecordedAt(rs.getObject("recorded_at", LocalDateTime.class));
                    return null;
                },
                userId,
                leaseContractId,
                userId,
                userId
        );
    }

    private String normalizeOccupantIntention(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (List.of("FOLLOW_PRIMARY_MOVE_OUT", "JOIN_RENEWAL").contains(normalized)) {
            return normalized;
        }
        throw new AppException(ApiErrorCode.MIGRATED_Y_DINH_NGUOI_O_CUNG_KHONG_HOP_LE);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void assertOwnerOrAssignedManagerCanAccessContract(Long leaseContractId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        if (principal.getRole() == Role.OWNER) {
            return;
        }
        if (principal.getRole() != Role.MANAGER) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
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
            throw new AppException(ApiErrorCode.CONTRACT_NOT_FOUND);
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
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
    }

    public record LeaseContractLiquidationRequest(
            LocalDate liquidationDate,
            String reason,
            String liquidationMode,
            List<Long> leavingProfileIds,
            List<Long> stayingProfileIds,
            Long replacementPrimaryTenantProfileId,
            @Valid
            List<LeaseContractLiquidationChargeRequest> charges
    ) {
    }

    public record LeaseContractLiquidationChargeRequest(
    @NotNull(message = "Loại phí thanh lý không được để trống.")
            InvoiceLineType lineType,
    @Size(max = 1000, message = "Mô tả phí thanh lý không được vượt quá 1000 ký tự.")
            String description,
    @PositiveOrZero(message = "Số lượng không được âm.")
            Integer quantity,
    @PositiveOrZero(message = "Đơn giá không được âm.")
            Long unitPrice,
    @PositiveOrZero(message = "Chỉ số cũ không được âm.")
            BigDecimal previousValue,
    @PositiveOrZero(message = "Chỉ số mới không được âm.")
            BigDecimal currentValue,
            Long photoFileId
    ) {
    }

    private List<LiquidationChargeInput> liquidationCharges(LeaseContractLiquidationRequest request) {
        if (request == null || request.charges() == null) {
            return null;
        }
        return request.charges().stream()
                .map(charge -> new LiquidationChargeInput(
                        charge.lineType(),
                        charge.description(),
                        charge.quantity(),
                        charge.unitPrice(),
                        charge.previousValue(),
                        charge.currentValue(),
                        charge.photoFileId()
                ))
                .toList();
    }

    public record AddCoOccupantRequest(
            Long tenantProfileId,
    @Size(max = 255, message = "Tên người ở cùng không được vượt quá 255 ký tự.")
            String fullName,
            LocalDate dob,
            Gender gender,
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
            String phone,
    @Email(message = "Email người ở cùng không hợp lệ.")
    @Size(max = 255, message = "Email người ở cùng không được vượt quá 255 ký tự.")
            String email,
    @Size(max = 1000, message = "Địa chỉ thường trú không được vượt quá 1000 ký tự.")
            String permanentAddress,
            LocalDate moveInDate,
    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }

    private ChangeRequestResponse toChangeRequestResponse(ChangeRequest req) {
        PersonProfile requesterProfile = req.getRequesterId() == null
                ? null
                : personProfileRepository.findByUserId(req.getRequesterId()).orElse(null);
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
                requesterProfile == null ? null : requesterProfile.getFullName(),
                requesterProfile == null ? null : requesterProfile.getPhone(),
                req.getResolutionNote(),
                req.getResolvedAt(),
                req.getCreatedAt()
        );
    }

    public record LeaseContractTermsUpdateRequest(
            @NotNull(message = "Ngày bắt đầu hợp đồng là bắt buộc.")
            LocalDate startDate,
            @NotNull
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
    @Min(value = 6, message = "Thời hạn gia hạn tối thiểu là 6 tháng.")
            Integer renewalTermMonths,
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

    public record OccupantIntentionRequest(
            @NotNull(message = "Ý định người ở cùng là bắt buộc.")
            String intention,
            @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự.")
            String note
    ) {
    }

    private record OccupantScope(Long contractOccupantId, Long tenantProfileId) {
    }
}
