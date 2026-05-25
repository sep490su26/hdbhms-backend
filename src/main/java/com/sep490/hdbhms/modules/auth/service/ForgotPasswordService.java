package com.sep490.hdbhms.modules.auth.service;

import com.sep490.hdbhms.common.AuditService;
import com.sep490.hdbhms.common.email.EmailService;
import com.sep490.hdbhms.common.exception.ApiException;
import com.sep490.hdbhms.modules.auth.dto.ForgotPasswordRequests;
import com.sep490.hdbhms.modules.auth.dto.ForgotPasswordResponses;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ForgotPasswordService {

    private static final int OTP_TTL_SECONDS = 300;
    private static final int RESET_TOKEN_TTL_SECONDS = 600;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int REQUEST_WINDOW_MINUTES = 10;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    public ForgotPasswordService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AuditService auditService,
            Environment environment
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditService = auditService;
        this.environment = environment;
    }

    @Transactional
    public ForgotPasswordResponses.RequestOtp requestOtp(ForgotPasswordRequests.RequestOtp request) {
        UserRow user = findUserByEmail(normalizeEmail(request.email()));
        validateAccountCanReset(user);
        validateRequestRate(user.email());
        validateMailConfiguration();

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                UPDATE password_reset_otps
                SET used_at = COALESCE(used_at, ?),
                    locked_at = COALESCE(locked_at, ?)
                WHERE user_id = ?
                  AND used_at IS NULL
                  AND locked_at IS NULL
                """, now, now, user.id());

        String otp = generateOtp();
        LocalDateTime expiresAt = now.plusSeconds(OTP_TTL_SECONDS);
        jdbcTemplate.update("""
                INSERT INTO password_reset_otps (user_id, email, otp_hash, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, user.id(), user.email(), passwordEncoder.encode(otp), expiresAt, now);

        emailService.sendForgotPasswordOtp(user.email(), otp, expiresAt);
        auditService.record(user.id(), "PASSWORD_RESET_OTP_REQUESTED", "USER", user.id());

        return new ForgotPasswordResponses.RequestOtp(
                "Mã OTP đã được gửi đến email của bạn",
                OTP_TTL_SECONDS
        );
    }

    @Transactional
    public ForgotPasswordResponses.VerifyOtp verifyOtp(ForgotPasswordRequests.VerifyOtp request) {
        UserRow user = findUserByEmail(normalizeEmail(request.email()));
        PasswordResetRow row = findLatestOtp(user.id())
                .orElseThrow(this::invalidOtp);

        LocalDateTime now = LocalDateTime.now();
        if (row.lockedAt() != null || row.usedAt() != null) {
            throw invalidOtp();
        }
        if (!row.expiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_OTP_EXPIRED", "Mã OTP đã hết hạn, vui lòng gửi lại");
        }

        if (!passwordEncoder.matches(request.otp(), row.otpHash())) {
            jdbcTemplate.update("""
                    UPDATE password_reset_otps
                    SET attempt_count = attempt_count + 1,
                        locked_at = CASE WHEN attempt_count + 1 >= ? THEN ? ELSE locked_at END
                    WHERE id = ?
                    """, MAX_OTP_ATTEMPTS, now, row.id());
            auditService.record(user.id(), "PASSWORD_RESET_FAILED", "USER", user.id());
            throw invalidOtp();
        }

        String resetToken = generateResetToken();
        jdbcTemplate.update("""
                UPDATE password_reset_otps
                SET reset_token_used_at = COALESCE(reset_token_used_at, ?)
                WHERE user_id = ?
                  AND reset_token_hash IS NOT NULL
                  AND reset_token_used_at IS NULL
                """, now, user.id());
        jdbcTemplate.update("""
                UPDATE password_reset_otps
                SET used_at = ?,
                    reset_token_hash = ?,
                    reset_token_expires_at = ?
                WHERE id = ?
                """, now, passwordEncoder.encode(resetToken), now.plusSeconds(RESET_TOKEN_TTL_SECONDS), row.id());
        auditService.record(user.id(), "PASSWORD_RESET_OTP_VERIFIED", "USER", user.id());

        return new ForgotPasswordResponses.VerifyOtp(resetToken, RESET_TOKEN_TTL_SECONDS);
    }

    @Transactional
    public ForgotPasswordResponses.ResetPassword resetPassword(ForgotPasswordRequests.ResetPassword request) {
        validateNewPassword(request.newPassword(), request.confirmPassword());
        UserRow user = findUserByEmail(normalizeEmail(request.email()));
        PasswordResetRow row = findLatestResetToken(user.id())
                .orElseThrow(this::invalidResetToken);

        LocalDateTime now = LocalDateTime.now();
        if (row.resetTokenExpiresAt() == null || !row.resetTokenExpiresAt().isAfter(now)) {
            throw invalidResetToken();
        }
        if (!passwordEncoder.matches(request.resetToken(), row.resetTokenHash())) {
            auditService.record(user.id(), "PASSWORD_RESET_FAILED", "USER", user.id());
            throw invalidResetToken();
        }

        jdbcTemplate.update("""
                UPDATE users
                SET password_hash = ?,
                    must_change_password = FALSE,
                    password_changed_at = ?,
                    updated_at = ?
                WHERE id = ?
                  AND deleted_at IS NULL
                """, passwordEncoder.encode(request.newPassword()), now, now, user.id());
        jdbcTemplate.update("""
                UPDATE password_reset_otps
                SET used_at = COALESCE(used_at, ?),
                    locked_at = COALESCE(locked_at, ?),
                    reset_token_used_at = COALESCE(reset_token_used_at, ?)
                WHERE user_id = ?
                """, now, now, now, user.id());
        auditService.record(user.id(), "PASSWORD_RESET_SUCCESS", "USER", user.id());

        return new ForgotPasswordResponses.ResetPassword("Đổi mật khẩu thành công");
    }

    private UserRow findUserByEmail(String email) {
        UserRow user = jdbcTemplate.query("""
                SELECT id, email, status
                FROM users
                WHERE LOWER(email) = LOWER(?)
                  AND deleted_at IS NULL
                LIMIT 1
                """, rs -> rs.next()
                ? new UserRow(rs.getLong("id"), rs.getString("email"), rs.getString("status"))
                : null, email);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "AUTH_EMAIL_NOT_FOUND", "Email chưa được đăng ký trong hệ thống");
        }
        return user;
    }

    private void validateAccountCanReset(UserRow user) {
        if ("ACTIVE".equals(user.status())) {
            return;
        }
        if ("PENDING_APPROVAL".equals(user.status()) || "PENDING_CONTRACT".equals(user.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_PENDING", "Tài khoản chưa được duyệt");
        }
        if ("DISABLED".equals(user.status()) || "REJECTED".equals(user.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_DISABLED", "Tài khoản không còn hoạt động");
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_DISABLED", "Tài khoản không còn hoạt động");
    }

    private void validateRequestRate(String email) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM password_reset_otps
                WHERE LOWER(email) = LOWER(?)
                  AND created_at >= ?
                """, Integer.class, email, LocalDateTime.now().minusMinutes(REQUEST_WINDOW_MINUTES));
        if (count != null && count >= MAX_REQUESTS_PER_WINDOW) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_OTP_RATE_LIMIT", "Bạn đã yêu cầu OTP quá nhiều lần, vui lòng thử lại sau");
        }
    }

    private java.util.Optional<PasswordResetRow> findLatestOtp(Long userId) {
        List<PasswordResetRow> rows = jdbcTemplate.query("""
                SELECT id, otp_hash, expires_at, used_at, attempt_count, locked_at,
                       reset_token_hash, reset_token_expires_at, reset_token_used_at
                FROM password_reset_otps
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """, (rs, rowNum) -> mapPasswordResetRow(rs), userId);
        return rows.stream().findFirst();
    }

    private java.util.Optional<PasswordResetRow> findLatestResetToken(Long userId) {
        List<PasswordResetRow> rows = jdbcTemplate.query("""
                SELECT id, otp_hash, expires_at, used_at, attempt_count, locked_at,
                       reset_token_hash, reset_token_expires_at, reset_token_used_at
                FROM password_reset_otps
                WHERE user_id = ?
                  AND reset_token_hash IS NOT NULL
                  AND reset_token_used_at IS NULL
                ORDER BY reset_token_expires_at DESC, id DESC
                LIMIT 3
                """, (rs, rowNum) -> mapPasswordResetRow(rs), userId);
        return rows.stream().findFirst();
    }

    private PasswordResetRow mapPasswordResetRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PasswordResetRow(
                rs.getLong("id"),
                rs.getString("otp_hash"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                nullableDateTime(rs, "used_at"),
                rs.getInt("attempt_count"),
                nullableDateTime(rs, "locked_at"),
                rs.getString("reset_token_hash"),
                nullableDateTime(rs, "reset_token_expires_at"),
                nullableDateTime(rs, "reset_token_used_at")
        );
    }

    private void validateMailConfiguration() {
        boolean hasUsername = StringUtils.hasText(environment.getProperty("GMAIL_USERNAME"))
                || StringUtils.hasText(environment.getProperty("spring.mail.username"));
        boolean hasPassword = StringUtils.hasText(environment.getProperty("GMAIL_APP_PASSWORD"))
                || StringUtils.hasText(environment.getProperty("spring.mail.password"));
        if (!hasUsername || !hasPassword) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AUTH_MAIL_NOT_CONFIGURED",
                    "Chưa cấu hình GMAIL_USERNAME/GMAIL_APP_PASSWORD"
            );
        }
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_PASSWORD_INVALID", "Mật khẩu mới phải có ít nhất 8 ký tự");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_PASSWORD_INVALID", "Mật khẩu mới phải có chữ và số");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_PASSWORD_CONFIRM_MISMATCH", "Xác nhận mật khẩu không khớp");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOtp() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiException invalidOtp() {
        return new ApiException(HttpStatus.BAD_REQUEST, "AUTH_OTP_INVALID", "Mã OTP không đúng hoặc đã hết hạn");
    }

    private ApiException invalidResetToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "AUTH_RESET_TOKEN_INVALID", "Reset token không hợp lệ hoặc đã hết hạn");
    }

    private LocalDateTime nullableDateTime(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private record UserRow(Long id, String email, String status) {
    }

    private record PasswordResetRow(
            Long id,
            String otpHash,
            LocalDateTime expiresAt,
            LocalDateTime usedAt,
            int attemptCount,
            LocalDateTime lockedAt,
            String resetTokenHash,
            LocalDateTime resetTokenExpiresAt,
            LocalDateTime resetTokenUsedAt
    ) {
    }
}
