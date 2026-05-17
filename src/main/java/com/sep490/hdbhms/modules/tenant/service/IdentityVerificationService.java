package com.sep490.hdbhms.modules.tenant.service;

import com.sep490.hdbhms.common.AuditService;
import com.sep490.hdbhms.common.exception.ApiException;
import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;
import com.sep490.hdbhms.modules.auth.service.OnboardingService;
import com.sep490.hdbhms.modules.tenant.dto.IdentityVerificationResponse;
import com.sep490.hdbhms.modules.user.entity.User;
import com.sep490.hdbhms.modules.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IdentityVerificationService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "heic");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/heic",
            "image/heif"
    );

    private final UserRepository userRepository;
    private final OnboardingService onboardingService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final Path storageDirectory;

    public IdentityVerificationService(
            UserRepository userRepository,
            OnboardingService onboardingService,
            AuditService auditService,
            JdbcTemplate jdbcTemplate,
            @Value("${app.file.storage.directory:${java.io.tmpdir}/hdbhms-files}") String storageDirectory
    ) {
        this.userRepository = userRepository;
        this.onboardingService = onboardingService;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.storageDirectory = Path.of(storageDirectory);
    }

    @Transactional
    public IdentityVerificationResponse uploadIdentity(
            Long userId,
            Long tenantId,
            MultipartFile portraitFile,
            MultipartFile idCardFrontFile,
            MultipartFile idCardBackFile
    ) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));

        if (!onboardingService.hasActiveTenant(user, tenantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật hồ sơ tenant này");
        }

        validateFile(portraitFile, "portrait_file");
        validateFile(idCardFrontFile, "id_card_front_file");
        validateFile(idCardBackFile, "id_card_back_file");

        Long personProfileId = onboardingService.resolveOrCreatePersonProfile(user);
        Long portraitFileId = storeFile(userId, portraitFile, "PORTRAIT_PHOTO", false);
        Long frontFileId = storeFile(userId, idCardFrontFile, "ID_CARD", true);
        Long backFileId = storeFile(userId, idCardBackFile, "ID_CARD", true);

        jdbcTemplate.update("""
                UPDATE person_profiles
                SET portrait_file_id = ?, updated_at = NOW(6)
                WHERE id = ?
                """, portraitFileId, personProfileId);

        upsertIdentityDocument(personProfileId, frontFileId, backFileId);

        auditService.record(userId, "IDENTITY_DOCUMENT_UPLOADED", "PERSON_PROFILE", personProfileId);
        auditService.record(userId, "PROFILE_COMPLETED", "PERSON_PROFILE", personProfileId);

        OnboardingStateResponse onboarding = onboardingService.resolve(user);
        return new IdentityVerificationResponse(
                "Cập nhật định danh thành công",
                onboarding.identityCompleted(),
                onboarding
        );
    }

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, fieldName + " là bắt buộc");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, fieldName + " vượt quá 10MB");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String contentType = file.getContentType();
        boolean allowedExtension = ALLOWED_EXTENSIONS.contains(extension);
        boolean allowedMime = !StringUtils.hasText(contentType)
                || ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT));

        if (!allowedExtension || !allowedMime) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, fieldName + " phải là jpg, jpeg, png hoặc heic");
        }
    }

    private Long storeFile(Long userId, MultipartFile file, String category, boolean sensitive) {
        try {
            byte[] bytes = file.getBytes();
            String extension = extensionOf(file.getOriginalFilename());
            String storageKey = "identity/%d/%s.%s".formatted(userId, UUID.randomUUID(), extension);
            Path target = storageDirectory.resolve(storageKey).normalize();
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);

            String checksum = sha256(bytes);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO file_metadata (
                            owner_user_id,
                            storage_key,
                            original_name,
                            mime_type,
                            size_bytes,
                            sha256_checksum,
                            category,
                            is_sensitive
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, userId);
                statement.setString(2, storageKey);
                statement.setString(3, file.getOriginalFilename());
                statement.setString(4, file.getContentType());
                statement.setLong(5, file.getSize());
                statement.setString(6, checksum);
                statement.setString(7, category);
                statement.setBoolean(8, sensitive);
                return statement;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("Cannot insert file metadata");
            }
            return key.longValue();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được file định danh");
        }
    }

    private void upsertIdentityDocument(Long personProfileId, Long frontFileId, Long backFileId) {
        Long existingDocumentId = jdbcTemplate.query("""
                SELECT id
                FROM identity_documents
                WHERE profile_id = ?
                  AND doc_type = 'CCCD'
                  AND status = 'ACTIVE'
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, personProfileId);

        if (existingDocumentId == null) {
            jdbcTemplate.update("""
                    INSERT INTO identity_documents (
                        profile_id,
                        doc_type,
                        doc_number,
                        front_file_id,
                        back_file_id,
                        status
                    )
                    VALUES (?, 'CCCD', ?, ?, ?, 'ACTIVE')
                    """, personProfileId, "PENDING-" + personProfileId, frontFileId, backFileId);
            return;
        }

        jdbcTemplate.update("""
                UPDATE identity_documents
                SET front_file_id = ?,
                    back_file_id = ?,
                    updated_at = NOW(6)
                WHERE id = ?
                """, frontFileId, backFileId, existingDocumentId);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String extensionOf(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
