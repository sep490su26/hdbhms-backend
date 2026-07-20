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

    public static String build(String roomCode, String tenantName, DocumentType type, LocalDate date) {
        String safeRoomCode = withRoomPrefix(normalize(roomCode, "Phong-X"));
        String safeDate = date == null ? "Chua-Ro-Ngay" : DATE_FORMATTER.format(date);
        String safeType = type == null ? "DOC" : type.name();
        return "%s_%s_%s.pdf".formatted(safeType, safeRoomCode, safeDate);
    }

    public static String attachmentContentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    private static String normalize(String value, String fallback) {
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
        return normalized;
    }

    private static String withRoomPrefix(String roomCode) {
        if (roomCode.startsWith("Phong")) {
            return roomCode;
        }
        if (roomCode.regionMatches(true, 0, "P", 0, 1)) {
            return "P" + roomCode.substring(1);
        }
        return "P" + roomCode;
    }

    public enum DocumentType {
        HDC,
        HDT,
        BBBG
    }
}
