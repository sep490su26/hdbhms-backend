package com.sep490.hdbhms.shared.utils;

import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestCodeBuilder {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd_MM_yyyy");

    public static String build(RequestType requestType, String roomCode, LocalDate createdDate) {
        String safeType = abbreviation(requestType);
        String safeRoomCode = withRoomPrefix(sanitize(roomCode, "X"));
        String safeDate = createdDate == null ? "Chua-Ro-Ngay" : DATE_FORMATTER.format(createdDate);
        return "%s_%s_%s".formatted(safeType, safeRoomCode, safeDate);
    }

    public static String withSequence(String code, int sequence) {
        if (sequence <= 1) return code;
        return code + "_" + sequence;
    }

    public static String nextAvailable(RequestType requestType, String roomCode, LocalDate createdDate, Predicate<String> exists) {
        String baseCode = build(requestType, roomCode, createdDate);
        for (int sequence = 1; sequence <= 999; sequence++) {
            String candidate = withSequence(baseCode, sequence);
            if (exists == null || !exists.test(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ApiErrorCode.REQUEST_CODE_ALLOCATION_FAILED);
    }

    public static String abbreviation(RequestType requestType) {
        if (requestType == null) return "YC";
        return switch (requestType) {
            case METER_READING_CORRECTION -> "DCCS";
            case INVOICE_ADJUSTMENT -> "DCHD";
            case RENT_PRICE_ADJUSTMENT -> "DCGT";
            case DEPOSIT_REFUND_REQUEST -> "HTC";
            case ROOM_TRANSFER -> "CP";
            case CONTRACT_LIQUIDATION -> "TLHD";
            case CONTRACT_RENEWAL -> "GHHD";
            case MOVE_OUT -> "TP";
            case COMPLAINT -> "KN";
            case PERMISSION_ACCESS -> "CQ";
            case TENANT_PROFILE_ACCESS -> "XHS";
            case ADD_CO_OCCUPANT -> "TNOC";
            case EXPENSE_APPROVAL -> "DYC";
        };
    }

    private static String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String sanitized = value.trim().replaceAll("[^a-zA-Z0-9_-]", "");
        return sanitized.isBlank() ? fallback : sanitized;
    }

    private static String withRoomPrefix(String roomCode) {
        if (roomCode.startsWith("Phong")) return roomCode;
        if (roomCode.regionMatches(true, 0, "P", 0, 1)) {
            return "P" + roomCode.substring(1);
        }
        return "P" + roomCode;
    }
}
