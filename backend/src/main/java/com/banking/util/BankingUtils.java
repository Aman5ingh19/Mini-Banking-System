package com.banking.util;

import java.security.SecureRandom;

public final class BankingUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private BankingUtils() {
    }

    /**
     * Generates a 12-digit unguessable account number with a fixed routing prefix.
     * Example: 4021XXXXXXXX
     */
    public static String generateRandomAccountNumber() {
        // Prefix with 4021 (demo branch indicator) followed by 8 random digits
        StringBuilder sb = new StringBuilder("4021");
        for (int i = 0; i < 8; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Safely masks an account number for display in logs and UI.
     * Example: 402187654321 -> "•••• •••• 4321"
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "••••";
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "•••• •••• " + lastFour;
    }
}
