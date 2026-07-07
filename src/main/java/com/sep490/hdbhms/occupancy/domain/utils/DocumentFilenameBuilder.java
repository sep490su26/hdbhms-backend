package com.sep490.hdbhms.occupancy.domain.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentFilenameBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    public static String build(String roomCode, String tenantName, String documentType, LocalDate expectedDate) {
        String safeRoom = withRoomPrefix(sanitize(roomCode, "Phong-X"));
        String safeDocumentType = sanitize(documentType, "HDC");
        String safeDate = expectedDate != null ? expectedDate.format(DATE_FORMATTER) : "Chua-Ro-Ngay";

        return String.format("%s_%s_%s.pdf", safeRoom, safeDocumentType, safeDate);
    }

    private static String sanitize(String input, String fallback) {
        if (input == null || input.trim().isEmpty()) return fallback;
        return input.trim().replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private static String withRoomPrefix(String roomCode) {
        if (roomCode.startsWith("Phong")) return roomCode;
        if (roomCode.regionMatches(true, 0, "P", 0, 1)) {
            return "P" + roomCode.substring(1);
        }
        return "P" + roomCode;
    }
}
