package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PasswordUtil}.
 *
 * These tests protect a security property rather than a business rule:
 * no plain-text password is ever stored, and a password can still be
 * verified afterwards. Both halves matter — hashing that could not be
 * verified would break login, and verification that accepted anything
 * would break security.
 */
@DisplayName("PasswordUtil")
class PasswordUtilTest {

    @Test
    @DisplayName("hashing the same password twice gives the same result (deterministic)")
    void hashIsDeterministic() {
        assertEquals(PasswordUtil.hash("password123"), PasswordUtil.hash("password123"));
    }

    @Test
    @DisplayName("different passwords produce different hashes")
    void differentPasswordsDifferentHashes() {
        assertNotEquals(PasswordUtil.hash("password123"), PasswordUtil.hash("password124"));
    }

    @Test
    @DisplayName("the hash never contains the original password")
    void hashDoesNotLeakPlainText() {
        String hash = PasswordUtil.hash("password123");
        assertFalse(hash.contains("password123"),
                "The stored hash must not contain the plain-text password.");
    }

    @Test
    @DisplayName("SHA-256 produces a 64-character hex digest")
    void hashHasExpectedFormat() {
        String hash = PasswordUtil.hash("password123");
        assertEquals(64, hash.length(), "SHA-256 hex digests are always 64 characters.");
        assertTrue(hash.matches("[0-9a-f]{64}"), "Expected lower-case hex, got: " + hash);
    }

    @Test
    @DisplayName("matches() accepts the correct password")
    void matchesAcceptsCorrectPassword() {
        String stored = PasswordUtil.hash("password123");
        assertTrue(PasswordUtil.matches("password123", stored));
    }

    @Test
    @DisplayName("matches() rejects a wrong password")
    void matchesRejectsWrongPassword() {
        String stored = PasswordUtil.hash("password123");
        assertFalse(PasswordUtil.matches("wrongpassword", stored));
    }

    @Test
    @DisplayName("matches() is case-sensitive about the password itself")
    void matchesIsCaseSensitive() {
        String stored = PasswordUtil.hash("password123");
        assertFalse(PasswordUtil.matches("PASSWORD123", stored));
    }

    @Test
    @DisplayName("matches() returns false for null input instead of throwing")
    void matchesHandlesNullSafely() {
        String stored = PasswordUtil.hash("password123");
        assertFalse(PasswordUtil.matches(null, stored));
        assertFalse(PasswordUtil.matches("password123", null));
        assertFalse(PasswordUtil.matches(null, null));
    }

    @Test
    @DisplayName("the seed accounts in schema.sql verify against the documented password")
    void seedAccountPasswordMatchesSchema() {
        // This is the hash inserted for 'admin' and 'reception1' by sql/schema.sql.
        // If this test fails, the seed data and the documented default password
        // ("password123") have drifted apart and nobody could log in.
        String seedHash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94";
        assertTrue(PasswordUtil.matches("password123", seedHash),
                "The seed hash in schema.sql no longer matches the documented default password.");
    }
}
