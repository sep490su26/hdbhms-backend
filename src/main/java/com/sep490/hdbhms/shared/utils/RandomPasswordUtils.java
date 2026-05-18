package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomPasswordUtils {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private static final String AMBIGUOUS = "0OIl1|";

    public static String generatePassword(int length, boolean useAllSets, boolean avoidAmbiguous) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4");
        }

        StringBuilder charPool = new StringBuilder();
        List<String> requiredSets = new ArrayList<>();

        if (useAllSets) {
            requiredSets.add(UPPER);
            requiredSets.add(LOWER);
            requiredSets.add(DIGITS);
            requiredSets.add(SYMBOLS);
            charPool.append(UPPER).append(LOWER).append(DIGITS).append(SYMBOLS);
        } else {
            requiredSets.add(UPPER);
            requiredSets.add(LOWER);
            requiredSets.add(DIGITS);
            charPool.append(UPPER).append(LOWER).append(DIGITS);
        }

        if (avoidAmbiguous) {
            for (char c : AMBIGUOUS.toCharArray()) {
                int idx;
                while ((idx = charPool.indexOf(String.valueOf(c))) != -1) {
                    charPool.deleteCharAt(idx);
                }
            }
            for (int i = 0; i < requiredSets.size(); i++) {
                String set = requiredSets.get(i);
                StringBuilder cleaned = new StringBuilder();
                for (char ch : set.toCharArray()) {
                    if (AMBIGUOUS.indexOf(ch) == -1) {
                        cleaned.append(ch);
                    }
                }
                requiredSets.set(i, cleaned.toString());
            }
        }

        SecureRandom random = new SecureRandom();
        char[] password = new char[length];

        int pos = 0;
        if (useAllSets) {
            for (String set : requiredSets) {
                if (set.isEmpty()) continue;
                password[pos++] = set.charAt(random.nextInt(set.length()));
            }
        }

        String pool = charPool.toString();
        for (; pos < length; pos++) {
            password[pos] = pool.charAt(random.nextInt(pool.length()));
        }

        for (int i = password.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = password[i];
            password[i] = password[j];
            password[j] = temp;
        }

        return new String(password);
    }

}