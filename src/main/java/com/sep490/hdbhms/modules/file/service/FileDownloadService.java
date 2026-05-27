//package com.sep490.hdbhms.modules.file.service;
//
//import com.sep490.hdbhms.common.exception.ApiException;
//import java.nio.file.Path;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.HttpStatus;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//@Service
//public class FileDownloadService {
//
//    private final JdbcTemplate jdbcTemplate;
//    private final Path storageDirectory;
//
//    public FileDownloadService(
//            JdbcTemplate jdbcTemplate,
//            @Value("${app.file.storage.directory:${java.io.tmpdir}/hdbhms-files}") String storageDirectory
//    ) {
//        this.jdbcTemplate = jdbcTemplate;
//        this.storageDirectory = resolveStorageDirectory(storageDirectory);
//    }
//
//    public PublicFile getPublicFile(Long fileId) {
//        FileRow file = jdbcTemplate.query("""
//                SELECT storage_key, mime_type, is_sensitive
//                FROM file_metadata
//                WHERE id = ?
//                  AND deleted_at IS NULL
//                """, rs -> rs.next()
//                ? new FileRow(
//                rs.getString("storage_key"),
//                rs.getString("mime_type"),
//                rs.getBoolean("is_sensitive")
//        )
//                : null, fileId);
//
//        if (file == null || file.isSensitive()) {
//            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy file");
//        }
//
//        Path path = storageDirectory.resolve(file.storageKey()).normalize();
//        if (!path.startsWith(storageDirectory.normalize())) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "Đường dẫn file không hợp lệ");
//        }
//
//        Resource resource = new FileSystemResource(path);
//        if (!resource.exists() || !resource.isReadable()) {
//            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy file");
//        }
//
//        return new PublicFile(resource, file.mimeType());
//    }
//
//    public PublicFile getTenantFile(Long userId, Long tenantId, Long fileId) {
//        FileRow file = jdbcTemplate.query("""
//                SELECT storage_key, mime_type, is_sensitive
//                FROM file_metadata
//                WHERE id = ?
//                  AND deleted_at IS NULL
//                """, rs -> rs.next()
//                ? new FileRow(
//                rs.getString("storage_key"),
//                rs.getString("mime_type"),
//                rs.getBoolean("is_sensitive")
//        )
//                : null, fileId);
//
//        if (file == null || !canCurrentTenantAccessFile(userId, tenantId, fileId)) {
//            throw new ApiException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y file");
//        }
//
//        Path path = storageDirectory.resolve(file.storageKey()).normalize();
//        if (!path.startsWith(storageDirectory.normalize())) {
//            throw new ApiException(HttpStatus.FORBIDDEN, "ÄÆ°á»ng dáº«n file khÃ´ng há»£p lá»‡");
//        }
//
//        Resource resource = new FileSystemResource(path);
//        if (!resource.exists() || !resource.isReadable()) {
//            throw new ApiException(HttpStatus.NOT_FOUND, "KhÃ´ng tÃ¬m tháº¥y file");
//        }
//
//        return new PublicFile(resource, file.mimeType());
//    }
//
//    private boolean canCurrentTenantAccessFile(Long userId, Long tenantId, Long fileId) {
//        Integer count = jdbcTemplate.queryForObject("""
//                SELECT COUNT(*)
//                FROM tenants t
//                JOIN users u ON u.id = t.user_id
//                JOIN person_profiles pp
//                  ON pp.deleted_at IS NULL
//                 AND (
//                    pp.phone = u.phone
//                    OR LOWER(pp.email) = LOWER(u.email)
//                    OR pp.id IN (
//                        SELECT lc.primary_tenant_profile_id
//                        FROM lease_contracts lc
//                        JOIN contract_occupants co ON co.contract_id = lc.id
//                        WHERE co.tenant_id = t.id
//                          AND lc.deleted_at IS NULL
//                    )
//                    OR pp.id IN (
//                        SELECT da.depositor_person_profile_id
//                        FROM deposit_agreements da
//                        WHERE da.tenant_id = t.id
//                          AND da.depositor_person_profile_id IS NOT NULL
//                    )
//                 )
//                LEFT JOIN identity_documents idoc
//                  ON idoc.profile_id = pp.id
//                 AND idoc.status = 'ACTIVE'
//                WHERE t.id = ?
//                  AND t.user_id = ?
//                  AND t.deleted_at IS NULL
//                  AND u.deleted_at IS NULL
//                  AND (
//                    pp.portrait_file_id = ?
//                    OR idoc.front_file_id = ?
//                    OR idoc.back_file_id = ?
//                  )
//                """, Integer.class, tenantId, userId, fileId, fileId, fileId);
//        return count != null && count > 0;
//    }
//
//    private Path resolveStorageDirectory(String storageDirectory) {
//        return StringUtils.hasText(storageDirectory)
//                ? Path.of(storageDirectory).toAbsolutePath().normalize()
//                : Path.of("").toAbsolutePath().normalize();
//    }
//
//    public record PublicFile(Resource resource, String mimeType) {
//    }
//
//    private record FileRow(String storageKey, String mimeType, boolean isSensitive) {
//    }
//}
