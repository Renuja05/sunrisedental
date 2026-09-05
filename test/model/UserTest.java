package model;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link User} role hierarchy (JUnit 4).
 *
 * The access-control rule from Task A ("only an Administrator may
 * manage staff accounts") is implemented as polymorphism rather than
 * as if/else checks scattered through the code, so it is tested here
 * at its source.
 */
public class UserTest {

    private Administrator admin() {
        return new Administrator("U0001", "admin", "hash", "System Administrator", true);
    }

    private Receptionist receptionist() {
        return new Receptionist("U0002", "reception1", "hash", "Nimali Perera", true);
    }

    @Test
    public void administratorCanManageStaff() {
        assertTrue(admin().canManageStaff());
    }

    @Test
    public void receptionistCannotManageStaff() {
        assertFalse(receptionist().canManageStaff());
    }

    @Test
    public void rolesMatchDatabaseValues() {
        assertEquals("ADMINISTRATOR", admin().getRole());
        assertEquals("RECEPTIONIST", receptionist().getRole());
    }

    @Test
    public void rolesHaveOwnDashboardTitles() {
        assertEquals("Administrator Dashboard", admin().getDashboardTitle());
        assertEquals("Receptionist Dashboard", receptionist().getDashboardTitle());
    }

    @Test
    public void polymorphismResolvesToSubclass() {
        User asUser = admin();
        assertTrue("Calling through a User reference must still use Administrator's behaviour.",
                asUser.canManageStaff());
    }

    @Test
    public void toMapOmitsPasswordHash() {
        Map<String, Object> map = admin().toMap();
        assertFalse("The password hash must never be serialised into an API response.",
                map.containsKey("passwordHash"));
        assertFalse("No part of the stored hash should appear in the client-facing map.",
                map.toString().contains("hash"));
    }

    @Test
    public void toMapIncludesClientFields() {
        Map<String, Object> map = receptionist().toMap();
        assertEquals("U0002", map.get("userId"));
        assertEquals("reception1", map.get("username"));
        assertEquals("Nimali Perera", map.get("fullName"));
        assertEquals("RECEPTIONIST", map.get("role"));
        assertEquals(Boolean.TRUE, map.get("active"));
    }

    @Test
    public void inactiveAccountIsFlagged() {
        Receptionist inactive = new Receptionist("U0003", "olduser", "hash", "Old User", false);
        assertFalse(inactive.isActive());
        assertEquals(Boolean.FALSE, inactive.toMap().get("active"));
    }

    @Test
    public void appointmentStatusConstantsMatchSchema() {
        assertEquals("SCHEDULED", Appointment.STATUS_SCHEDULED);
        assertEquals("COMPLETED", Appointment.STATUS_COMPLETED);
        assertEquals("CANCELLED", Appointment.STATUS_CANCELLED);
    }

    @Test
    public void appointmentToMapFormatsDateAndTimeAsIsoStrings() {
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
