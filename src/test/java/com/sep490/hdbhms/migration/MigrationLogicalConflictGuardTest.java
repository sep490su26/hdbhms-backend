package com.sep490.hdbhms.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationLogicalConflictGuardTest {

    @Test
    void roomTransferStatusMigrationConvertsLegacyValueBeforeRemovingIt() throws IOException {
        String sql = read("migration/dev/V10__align_room_transfer_request_status_enum.sql");
        int transitionalEnum = sql.indexOf("'WAITING_APPROVAL'");
        int dataUpdate = sql.indexOf("WHERE status = 'WAITING_APPROVAL'");
        int finalEnum = sql.lastIndexOf("MODIFY COLUMN status ENUM");

        assertTrue(transitionalEnum >= 0 && transitionalEnum < dataUpdate && dataUpdate < finalEnum);
    }

    @Test
    void permissionMigrationCopiesLegacyRowsAndSupportsAllJavaRequestTypes() throws IOException {
        String sql = read("migration/dev/V17__merge_permission_requests_into_change_requests.sql");

        assertTrue(sql.contains("'ADD_CO_OCCUPANT'"));
        assertTrue(sql.contains("INSERT INTO hdbhms.change_requests"));
        assertTrue(sql.contains("FROM hdbhms.permission_requests pr"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
    }

    @Test
    void approvedGenericPermissionRequestsCreateMigratedGrants() throws IOException {
        String sql = read("migration/dev/V18__add_permission_grants.sql");

        assertTrue(sql.contains("'TENANT_PROFILE_ACCESS', 'PERMISSION_ACCESS'"));
    }

    @Test
    void userStatusMigrationConvertsDisabledBeforeRemovingIt() throws IOException {
        String sql = read("migration/dev/V40__align_user_account_status_enum.sql");
        int transitionalEnum = sql.indexOf("'DISABLED'");
        int dataUpdate = sql.indexOf("WHERE status = 'DISABLED'");
        int finalEnum = sql.lastIndexOf("MODIFY COLUMN status ENUM");
        String finalDefinition = sql.substring(finalEnum);

        assertTrue(transitionalEnum >= 0 && transitionalEnum < dataUpdate && dataUpdate < finalEnum);
        assertTrue(finalDefinition.contains("'INACTIVE'"));
        assertTrue(!finalDefinition.contains("'DISABLED'"));
    }

    @Test
    void demoSeedUsesNormalizedVietnameseCodesAndCompleteSignerIdentityAssets() throws IOException {
        String v2 = read("migration/dev/V2__seed_initial_property_and_rules.sql");
        String v42 = read("migration/dev/V42__seed_demo_booking_checkout_lifecycle_exports.sql");
        String v44 = read("migration/dev/V44__backfill_seed_demo_payos_payment_intents.sql");
        String v48 = read("migration/dev/V48__seed_hai_dang_august_2026_electricity_collection.sql");
        String v54 = read("migration/dev/V54__repair_hai_dang_july_service_fee.sql");
        String v55 = read("migration/dev/V55__repair_hai_dang_july_lifecycle_service_fee.sql");
        String v56 = read("migration/dev/V56__align_hai_dang_service_fee_payment_cycles.sql");
        String v58 = read("migration/dev/V58__complete_hai_dang_expiry_notifications.sql");
        String normalizedSeed = String.join("\n", v42, v44, v48, v54, v55, v56, v58);

        assertTrue(v42.contains("HD_P401_01_07_2026_TP"));
        assertTrue(v42.contains("GD_P401_02_07_2026_TP_01"));
        assertTrue(v48.contains("'HDT_P'"));
        assertTrue(v48.contains("CONCAT('HD_P', room.room_code, '_01_08_2026_DV')"));
        assertFalse(normalizedSeed.contains("SEED-INV"));
        assertFalse(normalizedSeed.contains("SEED-TXN"));
        assertTrue(v2.contains("Dữ liệu khởi tạo: nhà trọ Hải Đăng 1"));
        assertFalse(v2.contains("Seed V1"));

        assertTrue(v42.contains("identity-samples/anh-chan-dung.webp"));
        assertTrue(v42.contains("identity-samples/cccd-mat-truoc.jpg"));
        assertTrue(v42.contains("identity-samples/cccd-mat-sau.jpg"));
        assertTrue(getClass().getClassLoader().getResource("static/identity-samples/anh-chan-dung.webp") != null);
        assertTrue(getClass().getClassLoader().getResource("static/identity-samples/cccd-mat-truoc.jpg") != null);
        assertTrue(getClass().getClassLoader().getResource("static/identity-samples/cccd-mat-sau.jpg") != null);
        assertTrue(v58.contains("SET profile.portrait_file_id = @hdd1_portrait_file_id"));
        assertTrue(v58.contains("SET identity_document.front_file_id = @hdd1_cccd_front_file_id"));
        assertTrue(v58.contains("INSERT INTO hdbhms.identity_documents"));
        assertTrue(v58.contains("WHERE signer.phone REGEXP '^0[0-9]{9}$'"));

        assertTrue(v48.contains("Nguyễn Văn Khải"));
        assertFalse(normalizedSeed.contains("Nguyen Van Khai"));
        assertFalse(normalizedSeed.contains("Tenant Demo"));
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
