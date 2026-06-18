package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants/profiles/me/files")
public class TenantFileController {

    private final DownloadFileUseCase downloadFileUseCase;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadMyTenantFile(@PathVariable Long fileId) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Long userId = principal.getId();
        com.sep490.hdbhms.identityandaccess.domain.value_objects.Role role = principal.getRole();

        boolean isManagerOrOwner = role == com.sep490.hdbhms.identityandaccess.domain.value_objects.Role.OWNER || role == com.sep490.hdbhms.identityandaccess.domain.value_objects.Role.MANAGER;

        if (!isManagerOrOwner) {
            // Verify if fileId belongs to the user's tenant profile
        // - portrait image from person_profiles
        // - front/back ID card from identity_documents
        // - vehicle image from vehicles
        // - deposit agreement contract from deposit_agreements
        // - lease contract from lease_contracts (not needed if done elsewhere, but we can allow it)
        Boolean isOwner = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN count(*) > 0 THEN 1 ELSE 0 END
                FROM person_profiles pp
                JOIN users u ON pp.user_id = u.id OR pp.phone = u.phone OR LOWER(pp.email) = LOWER(u.email)
                LEFT JOIN identity_documents idoc ON idoc.profile_id = pp.id
                LEFT JOIN vehicles v ON v.profile_id = pp.id
                WHERE u.id = ? AND u.deleted_at IS NULL AND pp.deleted_at IS NULL
                  AND (
                      pp.portrait_file_id = ?
                      OR idoc.front_file_id = ?
                      OR idoc.back_file_id = ?
                      OR v.image_file_id = ?
                  )
                """, Boolean.class, userId, fileId, fileId, fileId, fileId);

        if (Boolean.FALSE.equals(isOwner)) {
            // Check if it belongs to their lease contract or deposit agreement
            Boolean hasContract = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN count(*) > 0 THEN 1 ELSE 0 END
                FROM users u
                LEFT JOIN tenants t
                  ON t.user_id = u.id
                 AND t.deleted_at IS NULL
                LEFT JOIN person_profiles pp
                  ON pp.deleted_at IS NULL
                 AND (
                    pp.user_id = u.id
                    OR (u.phone IS NOT NULL AND pp.phone = u.phone)
                    OR (u.email IS NOT NULL AND LOWER(pp.email) = LOWER(u.email))
                 )
                LEFT JOIN lease_contracts lc
                  ON lc.deleted_at IS NULL
                 AND (
                    (
                        lc.primary_tenant_profile_id = pp.id
                        AND NOT EXISTS (
                            SELECT 1
                            FROM contract_occupants disabled_primary
                            WHERE disabled_primary.contract_id = lc.id
                              AND disabled_primary.tenant_profile_id = pp.id
                              AND disabled_primary.status = 'DISABLED'
                        )
                    )
                    OR lc.id IN (
                        SELECT co.contract_id
                        FROM contract_occupants co
                        WHERE co.tenant_profile_id = pp.id
                          AND co.status = 'ACTIVE'
                    )
                 )
                LEFT JOIN deposit_agreements da
                  ON da.depositor_person_profile_id = pp.id
                  OR da.tenant_id = t.id
                  OR da.id = lc.deposit_agreement_id
                WHERE u.id = ? AND u.deleted_at IS NULL
                  AND (
                      lc.contract_file_id = ?
                      OR da.contract_file_id = ?
                      OR da.signed_file_id = ?
                  )
                """, Boolean.class, userId, fileId, fileId, fileId);

            if (Boolean.FALSE.equals(hasContract)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this file");
            }
        }
        }

        var fileData = downloadFileUseCase.execute(new DownloadFileQuery(fileId));
        if (fileData == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, fileData.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(fileData.resource());
    }
}
