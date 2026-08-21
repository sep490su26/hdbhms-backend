package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetResidentOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetOnboardingStatusUseCase;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.IdentityDocumentEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PersonProfileEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaIdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetMyTenantProfileUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateMyTenantProfileUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateTenantProfileRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TenantProfileResponse;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantController {
    private static final Pattern CCCD_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    GetMyTenantProfileUseCase getMyTenantProfileUseCase;
    UpdateMyTenantProfileUseCase updateMyTenantProfileUseCase;
    TenantRepository tenantRepository;
    JpaPersonProfileRepository personProfileRepository;
    JpaIdentityDocumentRepository identityDocumentRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    UploadFileService uploadFileService;
    GetOnboardingStatusUseCase getOnboardingStatusUseCase;

    @GetMapping("/profiles/me")
    public ApiResponse<TenantProfileResponse> getMyTenantProfile() {
        return ApiResponse.<TenantProfileResponse>builder()
                .data(getMyTenantProfileUseCase.execute())
                .build();
    }

    @PutMapping("/profiles/me")
    public ApiResponse<TenantProfileResponse> updateMyTenantProfile(
            @Valid @RequestBody UpdateTenantProfileRequest request
    ) {
        return ApiResponse.<TenantProfileResponse>builder()
                .message("Cập nhật hồ sơ thành công")
                .data(updateMyTenantProfileUseCase.execute(request))
                .build();
    }

    @PostMapping(
            value = "/{tenantId}/me/identity-verification",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Transactional
    public ApiResponse<IdentityVerificationUploadResponse> uploadIdentityVerification(
            @PathVariable Long tenantId,
            @RequestPart("portraitFile") MultipartFile portraitFile,
            @RequestPart("idCardFrontFile") MultipartFile idCardFrontFile,
            @RequestPart("idCardBackFile") MultipartFile idCardBackFile,
            @RequestPart("docNumber") String docNumber,
            @RequestPart("issuedDate") String issuedDate,
            @RequestPart("issuedPlace") String issuedPlace,
            @RequestPart("permanentAddress") String permanentAddress,
            @RequestPart("email") String email
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException(ApiErrorCode.TENANT_NOT_FOUND));
        if (!userId.equals(tenant.getUserId())) {
            throw new AppException(ApiErrorCode.UNAUTHORIZED);
        }

        IdentityMetadata metadata = validateIdentityMetadata(
                docNumber,
                issuedDate,
                issuedPlace,
                permanentAddress,
                email
        );

        PersonProfileEntity profile = personProfileRepository.findByUser_IdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.USER_PROFILE_NOT_FOUND));

        FileMetadataEntity portrait = uploadIdentityFile(userId, portraitFile, FileCategory.PORTRAIT_PHOTO);
        FileMetadataEntity frontId = uploadIdentityFile(userId, idCardFrontFile, FileCategory.ID_CARD);
        FileMetadataEntity backId = uploadIdentityFile(userId, idCardBackFile, FileCategory.ID_CARD);

        profile.setPortraitFile(portrait);
        profile.setPermanentAddress(metadata.permanentAddress());
        profile.setEmail(metadata.email());
        personProfileRepository.save(profile);

        IdentityDocumentEntity identityDocument = identityDocumentRepository
                .findFirstByProfile_IdAndDocTypeAndStatusOrderByUpdatedAtDesc(
                        profile.getId(),
                        DocumentType.CCCD,
                        DocumentStatus.ACTIVE
                )
                .orElseGet(() -> IdentityDocumentEntity.builder()
                        .profile(profile)
                        .docType(DocumentType.CCCD)
                        .docNumber(metadata.docNumber())
                        .status(DocumentStatus.ACTIVE)
                        .build());
        identityDocument.setDocNumber(metadata.docNumber());
        identityDocument.setIssuedDate(metadata.issuedDate());
        identityDocument.setIssuedPlace(metadata.issuedPlace());
        identityDocument.setFrontFile(frontId);
        identityDocument.setBackFile(backId);
        identityDocumentRepository.save(identityDocument);

        OnboardingStatusResponse onboarding = getOnboardingStatusUseCase.ofResident(
                new GetResidentOnboardingStatusQuery(userId)
        );
        boolean identityCompleted = onboarding.getActions().stream()
                .anyMatch(action -> "IDENTITY_VERIFICATION".equals(action.getActionKey()) && action.isCompleted());

        return ApiResponse.<IdentityVerificationUploadResponse>builder()
                .data(new IdentityVerificationUploadResponse(
                        true,
                        "Hoàn tất hồ sơ thành công",
                        identityCompleted,
                        onboarding.isOnBoardingCompleted(),
                        onboarding,
                        portrait.getId(),
                        frontId.getId(),
                        backId.getId()
                ))
                .build();
    }

    private IdentityMetadata validateIdentityMetadata(
            String docNumber,
            String issuedDate,
            String issuedPlace,
            String permanentAddress,
            String email
    ) {
        String normalizedDocNumber = normalizeRequired(docNumber);
        if (!CCCD_PATTERN.matcher(normalizedDocNumber).matches()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        LocalDate normalizedIssuedDate;
        try {
            normalizedIssuedDate = LocalDate.parse(normalizeRequired(issuedDate));
        } catch (DateTimeParseException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (normalizedIssuedDate.isAfter(LocalDate.now())) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        String normalizedIssuedPlace = normalizeRequired(issuedPlace);
        if (normalizedIssuedPlace.length() > 255) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        String normalizedPermanentAddress = normalizeRequired(permanentAddress);
        if (normalizedPermanentAddress.length() > 1000) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        String normalizedEmail = normalizeRequired(email).toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 255 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        return new IdentityMetadata(
                normalizedDocNumber,
                normalizedIssuedDate,
                normalizedIssuedPlace,
                normalizedPermanentAddress,
                normalizedEmail
        );
    }

    private String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return value.trim();
    }

    private FileMetadataEntity uploadIdentityFile(
            Long userId,
            MultipartFile file,
            FileCategory category
    ) {
        var metadata = uploadFileService.execute(
                new UploadFileCommand(userId, file, category, true)
        );
        return fileMetadataRepository.findById(metadata.getId())
                .orElseThrow(() -> new AppException(ApiErrorCode.FILE_UPLOAD_FAILED));
    }

    public record IdentityVerificationUploadResponse(
            boolean success,
            String message,
            boolean identityCompleted,
            boolean profileCompleted,
            OnboardingStatusResponse onboarding,
            Long portraitFileId,
            Long idCardFrontFileId,
            Long idCardBackFileId
    ) {
    }

    private record IdentityMetadata(
            String docNumber,
            LocalDate issuedDate,
            String issuedPlace,
            String permanentAddress,
            String email
    ) {
    }
}
