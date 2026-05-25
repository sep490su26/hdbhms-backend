package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {
    private static final String[] NUMBERS = {
            "không", "một", "hai", "ba", "bốn",
            "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
            "", "nghìn", "triệu", "tỷ"
    };

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String getFilenameExtension(String path) {
        if (isEmpty(path)) {
            return null;
        }
        int extIndex = path.lastIndexOf(46);
        if (extIndex == -1) {
            return null;
        }
        int folderIndex = path.lastIndexOf(47);
        return folderIndex > extIndex ? null : path.substring(extIndex + 1);
    }

    public static String getTokenFromAuthorizationHeader(String bearerToken) {
        if (!StringUtils.isEmpty(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public static String normalize(String input) {
        return input == null ? null : input.toLowerCase().trim();
    }

    public static String toSlugUnderscore(String input) {
        if (input == null) {
            return null;
        }

        var decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);

        var diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = diacriticPattern.matcher(decomposed).replaceAll("");

        String slug = withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");

        if (slug.isEmpty()) {
            return "";
        }
        return slug;
    }

    public static String toSlugDash(String input) {
        if (input == null) {
            return null;
        }

        var decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);

        var diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = diacriticPattern.matcher(decomposed).replaceAll("");

        String slug = withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isEmpty()) {
            return "";
        }
        return slug;
    }

    public static String toVietnamesePriceString(long number) {
        if (number == 0) {
            return "không";
        }
        StringBuilder result = new StringBuilder();
        int unitIndex = 0;
        while (number > 0) {
            int block = (int) (number % 1000);
            if (block != 0) {
                String blockText = readBlock(block);

                if (!blockText.isEmpty()) {
                    result.insert(0,
                            blockText +
                                    (UNITS[unitIndex].isEmpty()
                                            ? ""
                                            : " " + UNITS[unitIndex])
                                    + " ");
                }
            }
            number /= 1000;
            unitIndex++;
        }

        return result.toString()
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String readBlock(int number) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int units = number % 10;

        StringBuilder sb = new StringBuilder();
        if (hundreds > 0) {
            sb.append(NUMBERS[hundreds]).append(" trăm");
        }

        String tensUnits = formatTensAndUnits(tens, units, hundreds > 0);
        if (!tensUnits.isEmpty()) {
            if (sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(tensUnits);
        }

        return sb.toString().trim();
    }

    private static String formatTensAndUnits(int tens, int units, boolean hasHundreds) {
        String tensPart = "";
        if (tens > 1) {
            tensPart = NUMBERS[tens] + " mươi";
        } else if (tens == 1) {
            tensPart = "mười";
        } else if (hasHundreds && units > 0) {
            tensPart = "lẻ";
        }

        String unitsPart = "";
        if (tens > 1 && units == 1) {
            unitsPart = "mốt";
        } else if (units == 5 && (tens > 0 || hasHundreds)) {
            unitsPart = "lăm";
        } else if (units > 0) {
            unitsPart = NUMBERS[units];
        }

        if (!tensPart.isEmpty() && !unitsPart.isEmpty()) {
            return tensPart + " " + unitsPart;
        } else if (!tensPart.isEmpty()) {
            return tensPart;
        } else {
            return unitsPart;
        }
    }
}
