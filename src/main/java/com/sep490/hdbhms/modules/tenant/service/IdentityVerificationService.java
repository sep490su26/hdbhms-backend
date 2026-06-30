package com.sep490.hdbhms.modules.tenant.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetResidentOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetOnboardingStatusUseCase;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;
import com.sep490.hdbhms.modules.tenant.dto.IdentityVerificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Statement;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class IdentityVerificationService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private final JdbcTemplate jdbcTemplate;
    private final UploadFileService uploadFileService;
    private final JpaFileMetadataRepository fileMetadataRepository;
    private final GetOnboardingStatusUseCase getOnboardingStatusUseCase;

    public IdentityVerificationService(
            JdbcTemplate jdbcTemplate,
            UploadFileService uploadFileService,
            JpaFileMetadataRepository fileMetadataRepository,
            GetOnboardingStatusUseCase getOnboardingStatusUseCase
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.uploadFileService = uploadFileService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.getOnboardingStatusUseCase = getOnboardingStatusUseCase;
    }

    public IdentityVerificationResponse uploadIdentity(
            Long userId,
            Long tenantId,
            MultipartFile portraitFile,
            MultipartFile idCardFrontFile,
            MultipartFile idCardBackFile
    ) {
        assertTenantBelongsToUser(userId, tenantId);
        validateImageFile(portraitFile);
        validateImageFile(idCardFrontFile);
        validateImageFile(idCardBackFile);

        Long profileId = resolveOrCreatePersonProfile(userId);
        Long portraitFileId = uploadAndTagFile(userId, portraitFile, FileCategory.PORTRAIT_PHOTO, true);
        Long frontFileId = uploadAndTagFile(userId, idCardFrontFile, FileCategory.ID_CARD, true);
        Long backFileId = uploadAndTagFile(userId, idCardBackFile, FileCategory.ID_CARD, true);

        jdbcTemplate.update("""
                        UPDATE person_profiles
                        SET portrait_file_id = ?,
                            updated_at = NOW(6)
                        WHERE person_profile_id = ?
                        """,
                portraitFileId,
                profileId
        );
        upsertIdentityDocument(profileId, frontFileId, backFileId);

        OnboardingStatusResponse onboarding = getOnboardingStatusUseCase.ofResident(
                new GetResidentOnboardingStatusQuery(userId)
        );
        boolean completed = onboarding.isOnBoardingCompleted();
        return new IdentityVerificationResponse(
                true,
                "Upload hồ sơ thành công",
                portraitFileId,
                frontFileId,
                backFileId,
                completed,
                completed,
                onboarding
        );
    }

    private void assertTenantBelongsToUser(Long userId, Long tenantId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM tenants
                        WHERE tenant_id = ?
                          AND user_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                tenantId,
                userId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật hồ sơ tenant này.");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng upload đủ ảnh chân dung và 2 mặt CCCD.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ảnh quá lớn, vui lòng chọn ảnh khác.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Định dạng ảnh không hợp lệ.");
        }
    }

    private Long resolveOrCreatePersonProfile(Long userId) {
        Long existingProfileId = jdbcTemplate.query("""
                        SELECT person_profile_id AS id
                        FROM person_profiles
                        WHERE user_id = ?
                          AND deleted_at IS NULL
                        ORDER BY person_profile_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId
        );
        if (existingProfileId != null) {
            return existingProfileId;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int affectedRows = jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                            INSERT INTO person_profiles (
                                user_id,
                                full_name,
                                phone,
                                email,
                                created_at,
                                updated_at
                            )
                            SELECT user_id,
                                   'Khách thuê',
                                   phone,
                                   email,
                                   NOW(6),
                                   NOW(6)
                            FROM users
                            WHERE user_id = ?
                              AND deleted_at IS NULL
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            return statement;
        }, keyHolder);
        if (affectedRows == 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ.");
        }
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được hồ sơ khách thuê.");
        }
        return key.longValue();
    }

    private Long uploadAndTagFile(
            Long userId,
            MultipartFile file,
            FileCategory category,
            boolean sensitive
    ) {
        var uploaded = uploadFileService.execute(new UploadFileCommand(userId, file, category, sensitive));
        var metadata = fileMetadataRepository.findById(uploaded.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được file định danh."));
        metadata.setCategory(category);
        metadata.setSensitive(sensitive);
        fileMetadataRepository.save(metadata);
        return metadata.getId();
    }

    private void upsertIdentityDocument(Long profileId, Long frontFileId, Long backFileId) {
        Long existingDocumentId = jdbcTemplate.query("""
                        SELECT identity_document_id AS id
                        FROM identity_documents
                        WHERE profile_id = ?
                          AND doc_type = 'CCCD'
                        ORDER BY updated_at DESC, identity_document_id DESC
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                profileId
        );
        if (existingDocumentId == null) {
            jdbcTemplate.update("""
                            INSERT INTO identity_documents (
                                profile_id,
                                doc_type,
                                doc_number,
                                front_file_id,
                                back_file_id,
                                status,
                                created_at,
                                updated_at
                            )
                            VALUES (?, 'CCCD', ?, ?, ?, 'ACTIVE', NOW(6), NOW(6))
                            """,
                    profileId,
                    "PENDING-" + profileId,
                    frontFileId,
                    backFileId
            );
            return;
        }

        jdbcTemplate.update("""
                        UPDATE identity_documents
                        SET front_file_id = ?,
                            back_file_id = ?,
                            status = 'ACTIVE',
                            updated_at = NOW(6)
                        WHERE identity_document_id = ?
                        """,
                frontFileId,
                backFileId,
                existingDocumentId
        );
    }
}
