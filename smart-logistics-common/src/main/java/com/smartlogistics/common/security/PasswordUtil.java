package com.smartlogistics.common.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire centralisé de hachage et vérification sécurisée des mots de passe (BCrypt).
 */
public final class PasswordUtil {
    private static final int BCRYPT_ROUNDS = 10;

    private PasswordUtil() {}

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
