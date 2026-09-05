package util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link PasswordUtil} (JUnit 4).
 *
 * These tests protect a security property rather than a business rule:
 * no plain-text password is ever stored, and a password can still be
 * verified afterwards. Both halves matter - hashing that could not be
 * verified would break login, and verification that accepted anything
 * would break security.
 */
public class PasswordUtilTest {

    @Test
    public void hashIsDeterministic() {
        assertEquals(PasswordUtil.hash("password123"), PasswordUtil.hash("password123"));
    }

    @Test
    public void differentPasswordsProduceDifferentHashes() {
        assertNotEquals(PasswordUtil.hash("password123"), PasswordUtil.hash("password124"));
    }

    @Test
    public void hashDoesNotLeakPlainText() {
        String hash = PasswordUtil.hash("password123");
        assertFalse("The stored hash must not contain the plain-text password.",
                hash.contains("password123"));
    }

    @Test
    public void hashIs64CharacterHex() {
        String hash = PasswordUtil.hash("password123");
        assertEquals("SHA-256 hex digests are always 64 characters.", 64, hash.length());
        assertTrue("Expected lower-case hex, got: " + hash, hash.matches("[0-9a-f]{64}"));
    }

    @Test
    public void matchesAcceptsCorrectPassword() {
        String stored = PasswordUtil.hash("password123");
        assertTrue(PasswordUtil.matches("password123", stored));
    }

    @Test
    public void matchesRejectsWrongPassword() {
        String stored = PasswordUtil.hash("password123");
        assertFalse(PasswordUtil.matches("wrongpassword", stored));
    }

    @Test
    public void matchesIsCaseSensitive() {
        String stored = PasswordUtil.hash("password123");
        assertFalse(PasswordUtil.matches("PASSWORD123", stored));
    }

    @Test
    public void matchesHandlesNullSafely() {
        String stored = PasswordUtil.hash("password123");
        assertFalse(PasswordUtil.matches(null, stored));
        assertFalse(PasswordUtil.matches("password123", null));
        assertFalse(PasswordUtil.matches(null, null));
    }

    /**
     * This test found a real defect: the hash seeded by sql/schema.sql was
     * 63 characters instead of 64, so neither default account could log in.
     */
    @Test
    public void seedAccountPasswordMatchesSchema() {
        String seedHash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
        assertTrue("The seed hash in schema.sql no longer matches the documented default password.",
                PasswordUtil.matches("password123", seedHash));
    }
}
