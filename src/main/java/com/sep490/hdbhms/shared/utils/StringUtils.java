package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

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
}
