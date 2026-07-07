package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.DepositAgreementStatus;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
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
import com.sep490.hdbhms.occupancy.application.service.DepositContractDocumentService;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.DepositAgreementManagementUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.DepositAgreementStatusUpdateRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositAgreementSignedFileResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositContractPreviewResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.DocumentFilenameBuilder;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
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
    private static final Set<DepositAgreementStatus> SIGNED_FILE_UPLOADABLE_STATUSES = EnumSet.of(
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONFIRMED,
            DepositAgreementStatus.CONVERTED_TO_LEASE,
            DepositAgreementStatus.EXTENDED
    );
    private static final Set<String> SIGNED_FILE_CONTENT_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    GetRoomDetailsUseCase getRoomDetailsUseCase;
    PropertyRepository propertyRepository;
    DepositFormRepository depositFormRepository;
    DepositAgreementRepository depositAgreementRepository;
    RoomRepository roomRepository;
    GetMyListDepositAgreementsUseCase getMyListDepositAgreementsUseCase;
    GetDepositAgreementDetailsUseCase getDepositAgreementDetailsUseCase;
    DepositContractDocumentService depositContractDocumentService;
    UploadFileUseCase uploadFileUseCase;
    DownloadFileUseCase downloadFileUseCase;
    JpaFileMetadataRepository fileMetadataRepository;

    @GetMapping
    public ApiResponse<PageResponse<DepositAgreementResponse>> getDepositAgreements(
            @RequestParam(required = false) DepositAgreementStatus status,
            @RequestParam(required = false) List<DepositAgreementStatus> statuses,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return listDepositAgreements(status, statuses, signedFrom, signedTo, pageable);
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<DepositAgreementResponse>> getMyDepositAgreements(
            @RequestParam(required = false) DepositAgreementStatus status,
            @RequestParam(required = false) List<DepositAgreementStatus> statuses,
            @RequestParam(required = false) LocalDateTime signedFrom,
            @RequestParam(required = false) LocalDateTime signedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return listDepositAgreements(status, statuses, signedFrom, signedTo, pageable);
    }

    private ApiResponse<PageResponse<DepositAgreementResponse>> listDepositAgreements(
            DepositAgreementStatus status,
            List<DepositAgreementStatus> statuses,
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
                                                statuses,
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
        return downloadDepositDraftPdf(depositAgreementId);
    }

    @GetMapping("/{depositAgreementId}/draft-preview")
    public ApiResponse<DepositContractPreviewResponse> previewDepositDraftContract(
            @PathVariable("depositAgreementId") Long depositAgreementId
    ) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        assertCanAccessContract(depositAgreement);
        return ApiResponse.<DepositContractPreviewResponse>builder()
                .data(depositContractDocumentService.previewDraft(depositAgreementId))
                .build();
    }

    @GetMapping("/{depositAgreementId}/draft-pdf")
    public ResponseEntity<Resource> downloadDepositDraftPdf(
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
        String filename = depositDocumentFilename(depositAgreement);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentContentDispositionWithFallback(filename))
                .body(fileData.resource());
    }

    @PostMapping(value = "/{depositAgreementId}/signed-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DepositAgreementSignedFileResponse> uploadSignedDepositFile(
            @PathVariable("depositAgreementId") Long depositAgreementId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "signedAt", required = false) LocalDateTime signedAt,
            @RequestParam(value = "note", required = false) String note
    ) {
        assertOwnerOrManager();
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn file hợp đồng đặt cọc đã ký.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !SIGNED_FILE_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ PDF hoặc ảnh JPG/PNG/WEBP.");
        }

        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        if (!SIGNED_FILE_UPLOADABLE_STATUSES.contains(depositAgreement.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ upload bản đã ký sau khi tiền cọc đã được thanh toán/xác nhận.");
        }

        Long currentUserId = AuthUtils.getCurrentAuthenticationId();
        com.sep490.hdbhms.file.domain.model.FileMetadata uploaded;
        try {
            uploaded = uploadFileUseCase.execute(
                    new UploadFileCommand(currentUserId, file, FileCategory.DEPOSIT_CONTRACT, true)
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file hợp đồng đặt cọc đã ký.");
        }

        depositAgreement.attachSignedFile(uploaded.getId(), currentUserId, signedAt);
        DepositAgreement saved = depositAgreementRepository.save(depositAgreement);

        return ApiResponse.<DepositAgreementSignedFileResponse>builder()
                .data(DepositAgreementSignedFileResponse.builder()
                        .depositAgreementId(saved.getId())
                        .depositCode(saved.getDepositCode())
                        .signatureStatus(signatureStatus(saved))
                        .signedFileId(saved.getSignedFileId())
                        .signedFileName(signedFileName(saved.getSignedFileId()))
                        .signedAt(saved.getSignedAt())
                        .message("Tải lên bản hợp đồng đặt cọc đã ký thành công.")
                        .build())
                .build();
    }

    @GetMapping("/{depositAgreementId}/signed-file")
    public ResponseEntity<Resource> downloadSignedDepositFile(
            @PathVariable("depositAgreementId") Long depositAgreementId
    ) {
        DepositAgreement depositAgreement = getDepositAgreementDetailsUseCase.execute(
                new GetDepositAgreementDetailsQuery(depositAgreementId)
        );
        assertCanAccessContract(depositAgreement);
        if (depositAgreement.getSignedFileId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có bản hợp đồng đặt cọc đã ký.");
        }

        FileDataResponse fileData = downloadFileUseCase.execute(new DownloadFileQuery(depositAgreement.getSignedFileId()));
        if (fileData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file hợp đồng đặt cọc đã ký.");
        }
        String contentType = fileData.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : fileData.contentType();
        String filename = depositDocumentFilename(depositAgreement);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, DocumentFilenameBuilder.attachmentContentDisposition(filename))
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
                .contractDownloadUrl("/api/v1/deposit-agreements/" + depositAgreement.getId() + "/draft-pdf")
                .signatureStatus(signatureStatus(depositAgreement))
                .signatureStatusLabel(signatureStatusLabel(depositAgreement))
                .signedFileId(depositAgreement.getSignedFileId())
                .signedFileName(signedFileName(depositAgreement.getSignedFileId()))
                .signedAt(depositAgreement.getSignedAt())
                .signedUploadedById(depositAgreement.getSignedUploadedById())
                .signedFileDownloadUrl(signedFileDownloadUrl(depositAgreement))
                .canPreviewDraft(true)
                .canDownloadDraft(true)
                .canUploadSignedFile(canUploadSignedFile(depositAgreement))
                .canViewSignedFile(depositAgreement.getSignedFileId() != null)
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
                .contractDownloadUrl("/api/v1/deposit-agreements/" + depositAgreement.getId() + "/draft-pdf")
                .signatureStatus(signatureStatus(depositAgreement))
                .signatureStatusLabel(signatureStatusLabel(depositAgreement))
                .signedFileId(depositAgreement.getSignedFileId())
                .signedFileName(signedFileName(depositAgreement.getSignedFileId()))
                .signedAt(depositAgreement.getSignedAt())
                .signedUploadedById(depositAgreement.getSignedUploadedById())
                .signedFileDownloadUrl(signedFileDownloadUrl(depositAgreement))
                .canPreviewDraft(true)
                .canDownloadDraft(true)
                .canUploadSignedFile(canUploadSignedFile(depositAgreement))
                .canViewSignedFile(depositAgreement.getSignedFileId() != null)
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
        return fileId == null ? null : "/api/v1/files/private/" + fileId;
    }

    private String signedFileDownloadUrl(DepositAgreement depositAgreement) {
        return depositAgreement.getSignedFileId() == null
                ? null
                : "/api/v1/deposit-agreements/" + depositAgreement.getId() + "/signed-file";
    }

    private String attachmentContentDispositionWithFallback(String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String fallbackFilename = filename.replace("\\", "").replace("\"", "");
        return "attachment; filename=\"" + fallbackFilename + "\"; filename*=UTF-8''" + encodedFilename;
    }

    private String depositDocumentFilename(DepositAgreement depositAgreement) {
        Room room = getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(depositAgreement.getRoomId()));
        DepositForm depositForm = getDepositForm(depositAgreement);
        return com.sep490.hdbhms.occupancy.domain.utils.DocumentFilenameBuilder.build(
                room != null ? room.getRoomCode() : null,
                null,
                "HDC",
                resolveExpectedMoveInDate(depositAgreement, depositForm)
        );
    }

    private String signatureStatus(DepositAgreement depositAgreement) {
        return depositAgreement.getSignedFileId() == null ? "PENDING_SIGNATURE" : "SIGNED";
    }

    private String signatureStatusLabel(DepositAgreement depositAgreement) {
        return depositAgreement.getSignedFileId() == null ? "Chờ ký" : "Đã ký";
    }

    private boolean canUploadSignedFile(DepositAgreement depositAgreement) {
        return SIGNED_FILE_UPLOADABLE_STATUSES.contains(depositAgreement.getStatus());
    }

    private String signedFileName(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return fileMetadataRepository.findById(fileId)
                .map(file -> file.getOriginalName() != null ? file.getOriginalName() : "deposit-contract-signed-" + fileId)
                .orElse(null);
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

