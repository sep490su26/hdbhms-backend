package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentFilenameBuilder {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[/\\\\:*?\"<>|#]");
    private static final Pattern NON_PRINTABLE_ASCII = Pattern.compile("[^\\x20-\\x7E]");
    private static final int MAX_TENANT_NAME_LENGTH = 40;

    public static String build(String roomCode, String tenantName, DocumentType type, LocalDate date) {
        String safeRoomCode = normalize(roomCode, "Phong", false);
        String safeTenantName = normalize(tenantName, "Khach-Thue", true);
        String safeDate = date == null ? "ngay-chua-xac-dinh" : DATE_FORMATTER.format(date);
        String safeType = type == null ? "DOC" : type.name();
        return "P%s_%s_%s_%s.pdf".formatted(safeRoomCode, safeTenantName, safeType, safeDate);
    }

    public static String attachmentContentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    private static String normalize(String value, String fallback, boolean limitTenantName) {
        String input = value == null || value.isBlank() ? fallback : value.trim();
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        normalized = UNSAFE_CHARS.matcher(normalized).replaceAll("");
        normalized = NON_PRINTABLE_ASCII.matcher(normalized).replaceAll("");
        normalized = normalized.trim();
        if (normalized.isBlank()) {
            normalized = fallback;
        }
        if (limitTenantName && normalized.length() > MAX_TENANT_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_TENANT_NAME_LENGTH);
        }
        return normalized;
    }

    public enum DocumentType {
        HDC,
        HDT,
        BBBG
    }
}
