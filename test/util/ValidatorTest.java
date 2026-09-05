package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Validator}.
 *
 * Validator is the highest-value class to test in the whole system: it
 * is pure logic with no database or network dependency, it is the sole
 * defence against the "billing errors" and bad data described in the
 * scenario, and every one of its rules is a single, checkable
 * statement.
 *
 * Convention under test: every Validator method returns {@code null}
 * when the value is VALID, and a human-readable error message when it
 * is INVALID. So {@code assertNull} means "accepted" and
 * {@code assertNotNull} means "rejected".
 */
@DisplayName("Validator")
class ValidatorTest {

    @Nested
    @DisplayName("requiredText()")
    class RequiredText {

        @Test
        @DisplayName("accepts ordinary text")
        void acceptsText() {
            assertNull(Validator.requiredText("Nimali", "Name"));
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertNotNull(Validator.requiredText(null, "Name"));
        }

        @Test
        @DisplayName("rejects an empty string")
        void rejectsEmpty() {
            assertNotNull(Validator.requiredText("", "Name"));
        }

        @Test
        @DisplayName("rejects whitespace only")
        void rejectsWhitespaceOnly() {
            assertNotNull(Validator.requiredText("    ", "Name"));
        }

        @Test
        @DisplayName("includes the field label in the message, so the user knows which box to fix")
        void messageNamesTheField() {
            String message = Validator.requiredText("", "Contact number");
            assertTrue(message.contains("Contact number"),
                    "Expected the error to name the field, but got: " + message);
        }
    }

    @Nested
    @DisplayName("patientName()")
    class PatientName {

        @ParameterizedTest
        @ValueSource(strings = {
                "Nimali Perera",
                "John",
                "Mary-Jane Watson",
                "O'Brien",
                "Dr. S. Fernando"
        })
        @DisplayName("accepts realistic names, including hyphens, apostrophes and full stops")
        void acceptsValidNames(String name) {
            assertNull(Validator.patientName(name), "Should have accepted: " + name);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "N",             // too short (pattern requires 2+ characters)
                "123",           // digits only
                "Nimali99",      // digits inside a name
                "9Nimali",       // must start with a letter
                "<script>",      // punctuation not allowed by the pattern
                "Ann@Perera"
        })
        @DisplayName("rejects names containing digits or unexpected symbols")
        void rejectsInvalidNames(String name) {
            assertNotNull(Validator.patientName(name), "Should have rejected: " + name);
        }

        @Test
        @DisplayName("rejects a name longer than 80 characters")
        void rejectsOverlyLongName() {
            String tooLong = "A".repeat(81);
            assertNotNull(Validator.patientName(tooLong));
        }

        @Test
        @DisplayName("accepts a name of exactly 80 characters (upper boundary)")
        void acceptsMaximumLengthName() {
            String exactly80 = "A".repeat(80);
            assertNull(Validator.patientName(exactly80));
        }
    }

    @Nested
    @DisplayName("contactNumber()")
    class ContactNumber {

        @ParameterizedTest
        @ValueSource(strings = {"0771234567", "0112345678", "+94771234567"})
        @DisplayName("accepts valid Sri Lankan local and international formats")
        void acceptsValidNumbers(String number) {
            assertNull(Validator.contactNumber(number), "Should have accepted: " + number);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "077123456",      // 9 digits - one too few
                "07712345678",    // 11 digits - one too many
                "1771234567",     // does not start with 0
                "+9477123456",    // international form, too few digits
                "077-123-4567",   // hyphens not accepted
                "077 123 4567",   // spaces not accepted
                "abcdefghij"
        })
        @DisplayName("rejects wrong lengths, wrong prefixes and separator characters")
        void rejectsInvalidNumbers(String number) {
            assertNotNull(Validator.contactNumber(number), "Should have rejected: " + number);
        }
    }

    @Nested
    @DisplayName("username()")
    class Username {

        @ParameterizedTest
        @ValueSource(strings = {"admin", "reception1", "a_b_c", "Renuja05"})
        @DisplayName("accepts letters, digits and underscores after a leading letter")
        void acceptsValidUsernames(String username) {
            assertNull(Validator.username(username), "Should have accepted: " + username);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ab",                       // shorter than 3 characters
                "1admin",                   // must start with a letter
                "_admin",                   // must start with a letter
                "admin user",               // no spaces
                "admin!",                   // no punctuation
                "abcdefghijklmnopqrstuvwxyz" // longer than 20 characters
        })
        @DisplayName("rejects usernames breaking the length, prefix or character rules")
        void rejectsInvalidUsernames(String username) {
            assertNotNull(Validator.username(username), "Should have rejected: " + username);
        }
    }

    @Nested
    @DisplayName("password()")
    class Password {

        @Test
        @DisplayName("accepts a password of exactly the 6-character minimum")
        void acceptsMinimumLength() {
            assertNull(Validator.password("abc123"));
        }

        @Test
        @DisplayName("rejects a 5-character password (just below the boundary)")
        void rejectsTooShort() {
            assertNotNull(Validator.password("abc12"));
        }

        @Test
        @DisplayName("rejects an empty password")
        void rejectsEmpty() {
            assertNotNull(Validator.password(""));
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertNotNull(Validator.password(null));
        }
    }

    @Nested
    @DisplayName("appointmentDate()")
    class AppointmentDate {

        @Test
        @DisplayName("accepts today")
        void acceptsToday() {
            assertNull(Validator.appointmentDate(LocalDate.now()));
        }

        @Test
        @DisplayName("accepts a date next week")
        void acceptsNearFuture() {
            assertNull(Validator.appointmentDate(LocalDate.now().plusWeeks(1)));
        }

        @Test
        @DisplayName("rejects yesterday - appointments cannot be booked in the past")
        void rejectsPastDate() {
            assertNotNull(Validator.appointmentDate(LocalDate.now().minusDays(1)));
        }

        @Test
        @DisplayName("rejects a date more than 6 months ahead")
        void rejectsTooFarFuture() {
            assertNotNull(Validator.appointmentDate(LocalDate.now().plusMonths(7)));
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertNotNull(Validator.appointmentDate(null));
        }
    }

    @Nested
    @DisplayName("appointmentTime()")
    class AppointmentTime {

        @Test
        @DisplayName("accepts 08:00, the moment the clinic opens (lower boundary)")
        void acceptsOpeningTime() {
            assertNull(Validator.appointmentTime(LocalTime.of(8, 0)));
        }

        @Test
        @DisplayName("accepts 16:45, the last bookable slot before closing")
        void acceptsLastSlot() {
            assertNull(Validator.appointmentTime(LocalTime.of(16, 45)));
        }

        @Test
        @DisplayName("rejects 07:45, before the clinic opens")
        void rejectsBeforeOpening() {
            assertNotNull(Validator.appointmentTime(LocalTime.of(7, 45)));
        }

        @Test
        @DisplayName("rejects 17:00, the moment the clinic closes")
        void rejectsClosingTime() {
            assertNotNull(Validator.appointmentTime(LocalTime.of(17, 0)));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 15, 30, 45})
        @DisplayName("accepts every valid 15-minute slot boundary")
        void acceptsQuarterHourSlots(int minute) {
            assertNull(Validator.appointmentTime(LocalTime.of(10, minute)));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 7, 20, 31, 44, 59})
        @DisplayName("rejects times that do not fall on a 15-minute slot")
        void rejectsOffSlotTimes(int minute) {
            assertNotNull(Validator.appointmentTime(LocalTime.of(10, minute)));
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertNotNull(Validator.appointmentTime(null));
        }
    }

    @Nested
    @DisplayName("address()")
    class Address {

        @Test
        @DisplayName("accepts a normal street address")
        void acceptsValidAddress() {
            assertNull(Validator.address("42 Galle Road, Colombo 03"));
        }

        @Test
        @DisplayName("rejects an address shorter than 5 characters")
        void rejectsTooShort() {
            assertNotNull(Validator.address("No.1"));
        }

        @Test
        @DisplayName("rejects an address longer than 200 characters")
        void rejectsTooLong() {
            assertNotNull(Validator.address("A".repeat(201)));
        }
    }

    @Nested
    @DisplayName("selection()")
    class Selection {

        @Test
        @DisplayName("accepts any non-null selection")
        void acceptsNonNull() {
            assertNull(Validator.selection("D0001", "Dentist"));
        }

        @Test
        @DisplayName("rejects null, i.e. nothing chosen in the drop-down")
        void rejectsNull() {
            assertNotNull(Validator.selection(null, "Dentist"));
        }

        @Test
        @DisplayName("names the field in the message")
        void messageNamesTheField() {
            String message = Validator.selection(null, "Treatment type");
            assertTrue(message.contains("Treatment type"), "Got: " + message);
        }
    }

    @Nested
    @DisplayName("Clinic opening hours constants")
    class ClinicHours {

        @Test
        @DisplayName("the clinic opens at 08:00 and closes at 17:00")
        void openingHoursAreAsSpecified() {
            assertEquals(LocalTime.of(8, 0), Validator.CLINIC_OPEN);
            assertEquals(LocalTime.of(17, 0), Validator.CLINIC_CLOSE);
        }
    }
}
