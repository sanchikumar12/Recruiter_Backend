package com.pi.auth.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String ALL = UPPER + LOWER + DIGITS;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {}

    public static String generateRandomPassword(int length) {
        if (length < 8) {
            length = 12;
        }

        List<Character> chars = new ArrayList<>();
        // Ensure at least one uppercase, one lowercase, and one digit
        chars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        chars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));

        // Fill remaining with random characters from alphanumeric pool (no special characters)
        for (int i = 3; i < length; i++) {
            chars.add(ALL.charAt(RANDOM.nextInt(ALL.length())));
        }

        // Shuffle
        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(chars.size());
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }
}
