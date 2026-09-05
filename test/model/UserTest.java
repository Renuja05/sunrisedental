package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link User} role hierarchy.
 *
 * The access-control rule from Task A ("only an Administrator may
 * manage staff accounts") is implemented as polymorphism rather than
 * as if/else checks scattered through the code, so it is tested here
 * at its source.
 */
@DisplayName("User role hierarchy")
class UserTest {

    private Administrator admin() {
        return new Administrator("U0001", "admin", "hash", "System Administrator", true);
    }

    private Receptionist receptionist() {
        return new Receptionist("U0002", "reception1", "hash", "Nimali Perera", true);
    }

    @Test
    @DisplayName("an Administrator may manage staff accounts")
    void administratorCanManageStaff() {
        assertTrue(admin().canManageStaff());
    }

    @Test
    @DisplayName("a Receptionist may NOT manage staff accounts")
    void receptionistCannotManageStaff() {
        assertFalse(receptionist().canManageStaff());
    }

    @Test
    @DisplayName("each role reports the discriminator stored in the database")
    void rolesMatchDatabaseValues() {
        assertEquals("ADMINISTRATOR", admin().getRole());
        assertEquals("RECEPTIONIST", receptionist().getRole());
    }

    @Test
    @DisplayName("each role has its own dashboard title")
    void rolesHaveOwnDashboardTitles() {
        assertEquals("Administrator Dashboard", admin().getDashboardTitle());
        assertEquals("Receptionist Dashboard", receptionist().getDashboardTitle());
    }

    @Test
    @DisplayName("polymorphism works: a User reference gives the subclass behaviour")
    void polymorphismResolvesToSubclass() {
        User asUser = admin();
        assertTrue(asUser.canManageStaff(),
                "Calling through a User reference must still use Administrator's behaviour.");
    }

    @Test
    @DisplayName("toMap() never exposes the password hash to the client")
    void toMapOmitsPasswordHash() {
        Map<String, Object> map = admin().toMap();
        assertFalse(map.containsKey("passwordHash"),
                "The password hash must never be serialised into an API response.");
        assertFalse(map.toString().contains("hash"),
                "No part of the stored hash should appear in the client-facing map.");
    }

    @Test
    @DisplayName("toMap() includes the fields the browser actually needs")
    void toMapIncludesClientFields() {
        Map<String, Object> map = receptionist().toMap();
        assertEquals("U0002", map.get("userId"));
        assertEquals("reception1", map.get("username"));
        assertEquals("Nimali Perera", map.get("fullName"));
        assertEquals("RECEPTIONIST", map.get("role"));
        assertEquals(true, map.get("active"));
    }

    @Test
    @DisplayName("a deactivated account is flagged as inactive")
    void inactiveAccountIsFlagged() {
        Receptionist inactive = new Receptionist("U0003", "olduser", "hash", "Old User", false);
        assertFalse(inactive.isActive());
        assertEquals(false, inactive.toMap().get("active"));
    }

    @Test
    @DisplayName("Appointment status constants match the values allowed by the database CHECK constraint")
    void appointmentStatusConstantsMatchSchema() {
        assertEquals("SCHEDULED", Appointment.STATUS_SCHEDULED);
        assertEquals("COMPLETED", Appointment.STATUS_COMPLETED);
        assertEquals("CANCELLED", Appointment.STATUS_CANCELLED);
    }

    @Test
    @DisplayName("Appointment.toMap() serialises dates and times as ISO strings")
    void appointmentToMapFormatsDateTime() {
        Appointment appointment = new Appointment(
                "APT2026-0001", "P0001", "D0001", "T0001",
                java.time.LocalDate.of(2026, 5, 20),
                java.time.LocalTime.of(9, 30),
                Appointment.STATUS_SCHEDULED);

        Map<String, Object> map = appointment.toMap();
        assertEquals("2026-05-20", map.get("appointmentDate"));
        assertEquals("09:30", map.get("appointmentTime"));
        assertNotNull(map.get("appointmentNumber"));
    }
}
