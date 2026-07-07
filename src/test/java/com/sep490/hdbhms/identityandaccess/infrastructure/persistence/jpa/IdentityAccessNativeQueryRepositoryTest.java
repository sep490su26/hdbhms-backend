package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityAccessNativeQueryRepositoryTest {

    private static final String CONTAINER_NAME = "hdbhms-repository-test-" + UUID.randomUUID();
    private static final String MYSQL_PASSWORD = "repository-test-password";

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startMysqlAndMigrateSchema() throws Exception {
        String jdbcUrl;
        try {
            jdbcUrl = startMysqlContainer();
        } catch (IOException | IllegalStateException exception) {
            throw new TestAbortedException("Docker MySQL is not available", exception);
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                jdbcUrl,
                "root",
                MYSQL_PASSWORD
        );
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:migration/dev")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .target("8")
                .load()
                .migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        insertLegacyMigrationRows();
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:migration/dev")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
    }

    @AfterAll
    static void stopMysqlContainer() throws Exception {
        try {
            runDocker(Duration.ofSeconds(30), "rm", "-f", CONTAINER_NAME);
        } catch (IllegalStateException ignored) {
        }
    }

    @BeforeEach
    void cleanRowsCreatedByThisTest() {
        jdbcTemplate.update("DELETE FROM login_history WHERE user_agent = 'repository-test'");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@repository-test.example'");
    }

    @Test
    void getAllIdsByAccountIdUsesUserIdAndReturnsLongIds() throws NoSuchMethodException {
        Method method = JpaLoginHistoryRepository.class.getMethod("getAllIdsByAccountId", Long.class);
        assertQueryDoesNotUseSchemaOrColumn(
                method,
                "account_id",
                "sep490.hdbhms",
                "hdbhms.");

        Long userId = insertUser("login-history-owner@repository-test.example", "0910000001");
        Long otherUserId = insertUser("login-history-other@repository-test.example", "0910000002");

        Long matchingId = insertLoginHistory(userId);
        insertLoginHistory(otherUserId);

        List<Long> ids = jdbcTemplate.queryForList(jdbcSql(method), Long.class, userId);

        assertEquals(List.of(matchingId), ids);
        assertInstanceOf(Long.class, ids.getFirst());
    }

    @Test
    void findIdsByFullTextUsesEmailPrefixWithoutFulltextIndex() throws NoSuchMethodException {
        Method method = JpaUserRepository.class.getMethod("findIdsByFullText", String.class);
        String query = query(method);
        assertTrue(query.contains("email LIKE CONCAT(?1, '%')"));
        assertFalse(query.contains("MATCH(email)"));
        assertFalse(query.contains("hdbhms."));

        Long matchingId = insertUser("prefix-search@repository-test.example", "0910000003");
        insertUser("not-prefix-search@repository-test.example", "0910000004");

        List<Long> ids = jdbcTemplate.queryForList(jdbcSql(method), Long.class, "prefix-search");

        assertEquals(List.of(matchingId), ids);
        assertInstanceOf(Long.class, ids.getFirst());
    }

    @Test
    void migrationsPreserveLegacyTransferAndPermissionRequests() {
        assertEquals(
                "WAITING_MANAGER_APPROVAL",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM room_transfer_requests WHERE request_code = 'TR-LEGACY-MIGRATION'",
                        String.class
                )
        );
        assertEquals(
                "PENDING",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM change_requests WHERE request_code = 'PR-LEGACY-7001'",
                        String.class
                )
        );
        assertEquals(
                1,
                jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM permission_grants pg
                        JOIN change_requests cr ON cr.change_request_id = pg.source_change_request_id
                        WHERE cr.request_code = 'PR-LEGACY-7002'
                          AND pg.target_type = 'FILE'
                          AND pg.target_id = 88
                        """, Integer.class)
        );
    }

    private static void insertLegacyMigrationRows() {
        jdbcTemplate.update("""
                INSERT INTO users (user_id, phone, email, password_hash, role, status, must_change_password)
                VALUES
                    (7000, '0910007000', 'legacy-manager@migration-test.example', 'encoded-password', 'MANAGER', 'ACTIVE', 0),
                    (7003, '0910007003', 'legacy-tenant@migration-test.example', 'encoded-password', 'TENANT', 'ACTIVE', 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO permission_requests (
                    permission_request_id, requester_user_id, target_type, target_id,
                    rejected_reason, status, decided_at, created_at
                ) VALUES
                    (7001, 7000, 'FILE', 77, '', 'PENDING', NULL, NOW(6)),
                    (7002, 7000, 'FILE', 88, '', 'APPROVED', NOW(6), NOW(6))
                """);
        jdbcTemplate.update("""
                INSERT INTO person_profiles (person_profile_id, user_id, full_name, phone, email)
                VALUES (7003, 7003, 'Legacy Tenant', '0910007003', 'legacy-tenant@migration-test.example')
                """);
        jdbcTemplate.update("""
                INSERT INTO tenants (tenant_id, user_id, property_id)
                SELECT 7003, 7003, property_id
                FROM properties
                ORDER BY property_id
                LIMIT 1
                """);
        jdbcTemplate.update("""
                INSERT INTO lease_contracts (
                    lease_contract_id, contract_code, room_id, primary_tenant_profile_id,
                    start_date, end_date, rent_start_date, monthly_rent, payment_cycle_months
                )
                SELECT 7003, 'LC-LEGACY-MIGRATION', room_id, 7003,
                       CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 1 YEAR), CURRENT_DATE,
                       listed_price, 1
                FROM rooms
                ORDER BY room_id
                LIMIT 1
                """);
        jdbcTemplate.update("""
                INSERT INTO room_transfer_requests (
                    room_transfer_request_id, request_code, requester_id, old_contract_id,
                    old_room_id, target_room_id, requested_transfer_date, status
                )
                SELECT 7001, 'TR-LEGACY-MIGRATION', 7003, 7003,
                       MIN(room_id), MAX(room_id), CURRENT_DATE, 'WAITING_APPROVAL'
                FROM rooms
                """);
    }

    private static Long insertUser(String email, String phone) {
        jdbcTemplate.update("""
                INSERT INTO users (phone, email, password_hash, role, status, must_change_password)
                VALUES (?, ?, ?, 'LEAD', 'PENDING_CONTRACT', 0)
                """, phone, email, "encoded-password");
        return jdbcTemplate.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private static Long insertLoginHistory(Long userId) {
        jdbcTemplate.update("""
                INSERT INTO login_history (user_id, status, method, ip_address, user_agent)
                VALUES (?, 'SUCCESS', 'PASSWORD', '127.0.0.1', 'repository-test')
                """, userId);
        return jdbcTemplate.queryForObject("""
                SELECT login_history_id
                FROM login_history
                WHERE user_id = ? AND user_agent = 'repository-test'
                ORDER BY login_history_id DESC
                LIMIT 1
                """, Long.class, userId);
    }

    private static void assertQueryDoesNotUseSchemaOrColumn(Method method, String... forbiddenFragments) {
        String query = query(method);
        for (String fragment : forbiddenFragments) {
            assertFalse(query.contains(fragment), () -> "Native query must not contain " + fragment + ": " + query);
        }
    }

    private static String query(Method method) {
        return method.getAnnotation(Query.class).value();
    }

    private static String jdbcSql(Method method) {
        return query(method).replace("?1", "?");
    }

    private static String startMysqlContainer() throws Exception {
        runDocker(
                Duration.ofMinutes(4),
                "run",
                "--detach",
                "--rm",
                "--name",
                CONTAINER_NAME,
                "-e",
                "MYSQL_ROOT_PASSWORD=" + MYSQL_PASSWORD,
                "-e",
                "MYSQL_DATABASE=hdbhms",
                "-p",
                "3306",
                "mysql:8.0"
        );
        String port = publishedPort();
        String jdbcUrl = "jdbc:mysql://localhost:" + port + "/hdbhms"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        waitForMysql(jdbcUrl);
        return jdbcUrl;
    }

    private static String publishedPort() throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            String output = runDocker(Duration.ofSeconds(10), "port", CONTAINER_NAME, "3306/tcp");
            String line = output.lines().findFirst().orElse("").trim();
            if (!line.isBlank()) {
                return line.substring(line.lastIndexOf(':') + 1);
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Timed out waiting for MySQL container port");
    }

    private static void waitForMysql(String jdbcUrl) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        SQLException lastException = null;
        while (System.nanoTime() < deadline) {
            try (var ignored = DriverManager.getConnection(jdbcUrl, "root", MYSQL_PASSWORD)) {
                return;
            } catch (SQLException exception) {
                lastException = exception;
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("Timed out waiting for MySQL container readiness", lastException);
    }

    private static String runDocker(Duration timeout, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Docker command timed out: " + String.join(" ", command));
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Docker command failed: " + String.join(" ", command) + "\n" + output);
        }
        return output;
    }
}
