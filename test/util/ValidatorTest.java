package util;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Validator} (JUnit 4).
 *
 * Validator is the highest-value class to test in the whole system: it
 * is pure logic with no database or network dependency, it is the sole
 * defence against the "billing errors" and bad data described in the
 * scenario, and every one of its rules is a single, checkable
 * statement.
 *
 * Convention under test: every Validator method returns {@code null}
 * when the value is VALID, and a human-readable error message when it
 * is INVALID. So assertNull means "accepted" and assertNotNull means
 * "rejected".
 *
 * NOTE: in JUnit 4 the optional message is the FIRST argument
 * (assertNull(message, value)), the opposite way round from JUnit 5.
 */
public class ValidatorTest {

    // ==================== requiredText() ====================

    @Test
    public void requiredText_acceptsOrdinaryText() {
        assertNull(Validator.requiredText("Nimali", "Name"));
    }

    @Test
    public void requiredText_rejectsNull() {
        assertNotNull(Validator.requiredText(null, "Name"));
    }

    @Test
    public void requiredText_rejectsEmptyString() {
        assertNotNull(Validator.requiredText("", "Name"));
    }

    @Test
    public void requiredText_rejectsWhitespaceOnly() {
        assertNotNull(Validator.requiredText("    ", "Name"));
    }

    @Test
    public void requiredText_messageNamesTheField() {
        String message = Validator.requiredText("", "Contact number");
        assertTrue("Expected the error to name the field, but got: " + message,
                message.contains("Contact number"));
    }

    // ==================== patientName() ====================

    @Test
    public void patientName_acceptsRealisticNames() {
        String[] valid = {"Nimali Perera", "John", "Mary-Jane Watson", "O'Brien", "Dr. S. Fernando"};
        for (String name : valid) {
            assertNull("Should have accepted: " + name, Validator.patientName(name));
        }
    }

    @Test
    public void patientName_rejectsDigitsAndSymbols() {
        String[] invalid = {"N", "123", "Nimali99", "9Nimali", "<script>", "Ann@Perera"};
        for (String name : invalid) {
            assertNotNull("Should have rejected: " + name, Validator.patientName(name));
        }
    }

    @Test
    public void patientName_rejectsNameLongerThan80Characters() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 81; i++) tooLong.append('A');
        assertNotNull(Validator.patientName(tooLong.toString()));
    }

    @Test
    public void patientName_acceptsExactly80Characters() {
        StringBuilder exactly80 = new StringBuilder();
        for (int i = 0; i < 80; i++) exactly80.append('A');
        assertNull(Validator.patientName(exactly80.toString()));
    }

    // ==================== contactNumber() ====================

    @Test
    public void contactNumber_acceptsValidSriLankanFormats() {
        String[] valid = {"0771234567", "0112345678", "+94771234567"};
        for (String number : valid) {
            assertNull("Should have accepted: " + number, Validator.contactNumber(number));
        }
    }

    @Test
    public void contactNumber_rejectsWrongLengthsAndSeparators() {
        String[] invalid = {"077123456", "07712345678", "1771234567",
                            "+9477123456", "077-123-4567", "077 123 4567", "abcdefghij"};
        for (String number : invalid) {
            assertNotNull("Should have rejected: " + number, Validator.contactNumber(number));
        }
    }

    // ==================== username() ====================

    @Test
    public void username_acceptsLettersDigitsUnderscores() {
        String[] valid = {"admin", "reception1", "a_b_c", "Renuja05"};
        for (String username : valid) {
            assertNull("Should have accepted: " + username, Validator.username(username));
        }
    }

    @Test
    public void username_rejectsBadLengthPrefixOrCharacters() {
        String[] invalid = {"ab", "1admin", "_admin", "admin user", "admin!",
                            "abcdefghijklmnopqrstuvwxyz"};
        for (String username : invalid) {
            assertNotNull("Should have rejected: " + username, Validator.username(username));
        }
    }

    // ==================== password() ====================

    @Test
    public void password_acceptsExactlySixCharacters() {
        assertNull(Validator.password("abc123"));
    }

    @Test
    public void password_rejectsFiveCharacters() {
        assertNotNull(Validator.password("abc12"));
    }

    @Test
    public void password_rejectsEmpty() {
        assertNotNull(Validator.password(""));
    }

    @Test
    public void password_rejectsNull() {
        assertNotNull(Validator.password(null));
    }

    // ==================== appointmentDate() ====================

    @Test
    public void appointmentDate_acceptsToday() {
        assertNull(Validator.appointmentDate(LocalDate.now()));
    }

    @Test
    public void appointmentDate_acceptsNextWeek() {
        assertNull(Validator.appointmentDate(LocalDate.now().plusWeeks(1)));
    }

    @Test
    public void appointmentDate_rejectsYesterday() {
        assertNotNull(Validator.appointmentDate(LocalDate.now().minusDays(1)));
    }

    @Test
    public void appointmentDate_rejectsMoreThanSixMonthsAhead() {
        assertNotNull(Validator.appointmentDate(LocalDate.now().plusMonths(7)));
    }

    @Test
    public void appointmentDate_rejectsNull() {
        assertNotNull(Validator.appointmentDate(null));
    }

    // ==================== appointmentTime() ====================

    @Test
    public void appointmentTime_acceptsOpeningTime0800() {
        assertNull(Validator.appointmentTime(LocalTime.of(8, 0)));
    }

    @Test
    public void appointmentTime_acceptsLastSlot1645() {
        assertNull(Validator.appointmentTime(LocalTime.of(16, 45)));
    }

    @Test
    public void appointmentTime_rejectsBeforeOpening0745() {
        assertNotNull(Validator.appointmentTime(LocalTime.of(7, 45)));
    }

    @Test
    public void appointmentTime_rejectsClosingTime1700() {
        assertNotNull(Validator.appointmentTime(LocalTime.of(17, 0)));
    }

    @Test
    public void appointmentTime_acceptsEveryQuarterHourSlot() {
        int[] validMinutes = {0, 15, 30, 45};
        for (int minute : validMinutes) {
            assertNull("Should have accepted minute " + minute,
                    Validator.appointmentTime(LocalTime.of(10, minute)));
        }
    }

    @Test
    public void appointmentTime_rejectsOffSlotTimes() {
        int[] invalidMinutes = {1, 7, 20, 31, 44, 59};
        for (int minute : invalidMinutes) {
            assertNotNull("Should have rejected minute " + minute,
                    Validator.appointmentTime(LocalTime.of(10, minute)));
        }
    }

    @Test
    public void appointmentTime_rejectsNull() {
        assertNotNull(Validator.appointmentTime(null));
    }

    // ==================== address() ====================

    @Test
    public void address_acceptsNormalStreetAddress() {
        assertNull(Validator.address("42 Galle Road, Colombo 03"));
    }

    @Test
    public void address_rejectsShorterThanFiveCharacters() {
        assertNotNull(Validator.address("No.1"));
    }

    @Test
    public void address_rejectsLongerThan200Characters() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 201; i++) tooLong.append('A');
        assertNotNull(Validator.address(tooLong.toString()));
    }

    // ==================== selection() ====================

    @Test
    public void selection_acceptsNonNull() {
        assertNull(Validator.selection("D0001", "Dentist"));
    }

    @Test
    public void selection_rejectsNull() {
        assertNotNull(Validator.selection(null, "Dentist"));
    }

    @Test
    public void selection_messageNamesTheField() {
        String message = Validator.selection(null, "Treatment type");
        assertTrue("Got: " + message, message.contains("Treatment type"));
    }

    // ==================== clinic hours ====================

    @Test
    public void clinicOpensAt0800AndClosesAt1700() {
        assertEquals(LocalTime.of(8, 0), Validator.CLINIC_OPEN);
        assertEquals(LocalTime.of(17, 0), Validator.CLINIC_CLOSE);
    }
}
