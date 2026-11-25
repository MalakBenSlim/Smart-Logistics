package com.smartlogistics.common.security;

import com.smartlogistics.common.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * Validateur d'entrées partagé entre la passerelle Web et les services RMI
 * pour garantir la validation côté serveur.
 */
public final class InputValidator {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+0-9\\s-]{8,20}$");

    private InputValidator() {}

    public static String requireNonBlank(String value, String fieldName) throws BusinessException {
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "Le champ '" + fieldName + "' est obligatoire.");
        }
        return value.trim();
    }

    public static String requireValidEmail(String email) throws BusinessException {
        String trimmed = requireNonBlank(email, "email").toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException("VALIDATION_ERROR", "Adresse email invalide.");
        }
        return trimmed;
    }

    public static String requireValidPhone(String phone) throws BusinessException {
        String trimmed = requireNonBlank(phone, "téléphone");
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException("VALIDATION_ERROR", "Numéro de téléphone invalide.");
        }
        return trimmed;
    }

    public static void requireStrongPassword(String password) throws BusinessException {
        if (password == null || password.length() < 6) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Le mot de passe doit contenir au moins 6 caractères.");
        }
    }

    public static int requirePositiveInt(int value, String fieldName) throws BusinessException {
        if (value <= 0) {
            throw new BusinessException("VALIDATION_ERROR",
                    "La valeur de '" + fieldName + "' doit être strictement positive.");
        }
        return value;
    }

    public static double requireNonNegativeDouble(double value, String fieldName) throws BusinessException {
        if (Double.isNaN(value) || value < 0) {
            throw new BusinessException("VALIDATION_ERROR",
                    "La valeur de '" + fieldName + "' ne peut pas être négative.");
        }
        return value;
    }

    public static void requireValidCoordinates(double lat, double lng) throws BusinessException {
        if (Double.isNaN(lat) || Double.isNaN(lng)
                || lat < -90.0 || lat > 90.0
                || lng < -180.0 || lng > 180.0) {
            throw new BusinessException("VALIDATION_ERROR", "Coordonnées GPS invalides.");
        }
    }
}
