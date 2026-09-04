package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes passwords with SHA-256 before they are stored or compared, so
 * that a plain-text password is never written to the database.
 */
public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every standard JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String plainText, String hash) {
        if (plainText == null || hash == null) return false;
        return hash(plainText).equalsIgnoreCase(hash);
    }
}
