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
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantController {
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
                .data(updateMyTenantProfileUseCase.execute(request))
                .build();
    }

    @PostMapping(
            value = "/{tenantId}/me/identity-verification",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<IdentityVerificationUploadResponse> uploadIdentityVerification(
            @PathVariable Long tenantId,
            @RequestPart("portraitFile") MultipartFile portraitFile,
            @RequestPart("idCardFrontFile") MultipartFile idCardFrontFile,
            @RequestPart("idCardBackFile") MultipartFile idCardBackFile
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

        PersonProfileEntity profile = personProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.USER_PROFILE_NOT_FOUND));

        FileMetadataEntity portrait = uploadIdentityFile(userId, portraitFile, FileCategory.PORTRAIT_PHOTO);
        FileMetadataEntity frontId = uploadIdentityFile(userId, idCardFrontFile, FileCategory.ID_CARD);
        FileMetadataEntity backId = uploadIdentityFile(userId, idCardBackFile, FileCategory.ID_CARD);

        profile.setPortraitFile(portrait);
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
                        .docNumber("PENDING-" + profile.getId())
                        .status(DocumentStatus.ACTIVE)
                        .build());
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
}
