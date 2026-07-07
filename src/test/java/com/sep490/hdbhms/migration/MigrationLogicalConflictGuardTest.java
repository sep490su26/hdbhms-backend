package com.sep490.hdbhms.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationLogicalConflictGuardTest {

    @Test
    void roomTransferStatusMigrationConvertsLegacyValueBeforeRemovingIt() throws IOException {
        String sql = read("migration/dev/V9__align_room_transfer_request_status_enum.sql");
        int transitionalEnum = sql.indexOf("'WAITING_APPROVAL'");
        int dataUpdate = sql.indexOf("WHERE status = 'WAITING_APPROVAL'");
        int finalEnum = sql.lastIndexOf("MODIFY COLUMN status ENUM");

        assertTrue(transitionalEnum >= 0 && transitionalEnum < dataUpdate && dataUpdate < finalEnum);
    }

    @Test
    void permissionMigrationCopiesLegacyRowsAndSupportsAllJavaRequestTypes() throws IOException {
        String sql = read("migration/dev/V16__merge_permission_requests_into_change_requests.sql");

        assertTrue(sql.contains("'ADD_CO_OCCUPANT'"));
        assertTrue(sql.contains("INSERT INTO hdbhms.change_requests"));
        assertTrue(sql.contains("FROM hdbhms.permission_requests pr"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
    }

    @Test
    void approvedGenericPermissionRequestsCreateMigratedGrants() throws IOException {
        String sql = read("migration/dev/V17__add_permission_grants.sql");

        assertTrue(sql.contains("'TENANT_PROFILE_ACCESS', 'PERMISSION_ACCESS'"));
    }

    private String read(String resource) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
