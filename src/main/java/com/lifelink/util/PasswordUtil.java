package com.lifelink.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple salted SHA-256 password hashing.
 *
 * This is intentionally lightweight per the project spec: "use password
 * hashing if practical, but do not let advanced security consume project
 * time." It is NOT production-grade (no bcrypt/argon2 dependency needed),
 * but it is far better than storing plaintext passwords.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** Returns "salt:hash" (both Base64), safe to store as a single column. */
    public static String hash(String plainPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = sha256(salt, plainPassword);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String plainPassword, String storedSaltAndHash) {
        if (storedSaltAndHash == null || !storedSaltAndHash.contains(":")) {
            return false;
        }
        String[] parts = storedSaltAndHash.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = sha256(salt, plainPassword);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static byte[] sha256(byte[] salt, String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(plainPassword.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to hash password", e);
        }
    }
}
