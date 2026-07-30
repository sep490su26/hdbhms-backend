package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import java.text.Normalizer;
import java.util.Locale;

public enum Gender {
    MALE,
    FEMALE,
    UNKNOWN,
    OTHER;

    public static Gender fromLabel(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (fold(value)) {
            case "male", "m", "nam" -> MALE;
            case "female", "f", "nu" -> FEMALE;
            case "other", "o", "khac" -> OTHER;
            default -> UNKNOWN;
        };
    }

    public String toVietnameseLabel() {
        return switch (this) {
            case MALE -> "Nam";
            case FEMALE -> "Nữ";
            case OTHER -> "Khác";
            case UNKNOWN -> null;
        };
    }

    private static String fold(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
