package dao;

import db.DBConnection;
import model.Appointment;
import model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * INTEGRATION tests for the DAO layer.
 *
 * Unlike the tests in test/util and test/model, these need a running
 * MySQL server with the sunrise_dental database created from
 * sql/schema.sql. Rather than failing when the database is absent,
 * every test calls {@code assumeTrue(...)} first, which SKIPS the test
 * instead — so the automated suite still runs cleanly on a machine
 * without a database (for example a marker's laptop), and only the
 * unit tests execute.
 *
 * The single most important test in this class is
 * {@link #rejectsDoubleBooking()}: it proves the fix for the exact
 * problem the scenario opens with ("This has resulted in double
 * bookings").
 *
 * NOTE: these tests write real rows into the database. Run
 * sql/schema.sql again afterwards to reset it to clean seed data
 * before demonstrating or submitting.
 */
@DisplayName("DAO integration tests (require a running MySQL database)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppointmentDAOIntegrationTest {

    private static boolean databaseAvailable;

    private final PatientDAO patientDAO = new PatientDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final DentistDAO dentistDAO = new DentistDAO();
    private final TreatmentDAO treatmentDAO = new TreatmentDAO();

    /** A date far enough ahead that it will not clash with seed or demo data. */
    private static final LocalDate TEST_DATE = LocalDate.now().plusDays(30);

    @BeforeAll
    static void checkDatabase() {
        databaseAvailable = DBConnection.getInstance().testConnection();
        if (!databaseAvailable) {
            System.out.println("SKIPPING DAO integration tests: no database connection. " +
                    "Start MySQL and run sql/schema.sql to enable them.");
        }
    }

    @Test
    @Order(1)
    @DisplayName("seed data from schema.sql is present (3 dentists, 6 treatments)")
    void seedDataIsPresent() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        assertEquals(3, dentistDAO.findAll().size(),
                "schema.sql should have inserted exactly 3 dentists.");
        assertEquals(6, treatmentDAO.findAll().size(),
                "schema.sql should have inserted exactly 6 treatment types.");
    }

    @Test
    @Order(2)
    @DisplayName("findOrCreate() creates a new patient the first time a contact number is seen")
    void createsNewPatient() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        String uniqueContact = "07" + System.currentTimeMillis() % 100000000L;
        Patient patient = patientDAO.findOrCreate("Test Patient", "1 Test Lane, Colombo", uniqueContact);

        assertNotNull(patient.getPatientId(), "A generated patient id should have been assigned.");
        assertTrue(patient.getPatientId().startsWith("P"), "Patient ids should start with P.");
        assertEquals("Test Patient", patient.getName());
    }

    @Test
    @Order(3)
    @DisplayName("findOrCreate() reuses the SAME patient record for a repeat contact number")
    void reusesExistingPatient() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        String uniqueContact = "07" + (System.currentTimeMillis() + 1) % 100000000L;

        Patient first = patientDAO.findOrCreate("Repeat Patient", "2 Test Lane", uniqueContact);
        Patient second = patientDAO.findOrCreate("Repeat Patient Updated", "3 New Lane", uniqueContact);

        assertEquals(first.getPatientId(), second.getPatientId(),
                "The same contact number must map to one patient record, not two. " +
                "This is the fix for the scenario's 'lost patient records' problem.");
        assertEquals("Repeat Patient Updated", second.getName(),
                "Re-registering should refresh the stored name.");
    }

    @Test
    @Order(4)
    @DisplayName("register() saves a valid appointment and generates an appointment number")
    void registersAppointment() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        String contact = "07" + (System.currentTimeMillis() + 2) % 100000000L;
        Patient patient = patientDAO.findOrCreate("Booking Patient", "4 Test Lane", contact);

        Appointment appointment = appointmentDAO.register(
                patient.getPatientId(), "D0001", "T0001", TEST_DATE, LocalTime.of(9, 0));

        assertNotNull(appointment, "A free slot should produce a saved appointment, not null.");
        assertNotNull(appointment.getAppointmentNumber());
        assertTrue(appointment.getAppointmentNumber().startsWith("APT"),
                "Appointment numbers should start with APT.");
        assertEquals(Appointment.STATUS_SCHEDULED, appointment.getStatus());
        assertEquals("Dr. S. Fernando", appointment.getDentistName(),
                "The joined query should populate the dentist's name.");
    }

    @Test
    @Order(5)
    @DisplayName("register() REJECTS a second booking for the same dentist, date and time")
    void rejectsDoubleBooking() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        LocalTime slot = LocalTime.of(11, 15);
        String contactA = "07" + (System.currentTimeMillis() + 3) % 100000000L;
        String contactB = "07" + (System.currentTimeMillis() + 4) % 100000000L;

        Patient patientA = patientDAO.findOrCreate("Patient A", "5 Test Lane", contactA);
        Patient patientB = patientDAO.findOrCreate("Patient B", "6 Test Lane", contactB);

        Appointment first = appointmentDAO.register(
                patientA.getPatientId(), "D0002", "T0002", TEST_DATE, slot);
        assertNotNull(first, "The first booking of a free slot must succeed.");

        Appointment second = appointmentDAO.register(
                patientB.getPatientId(), "D0002", "T0003", TEST_DATE, slot);

        assertNull(second,
                "THE KEY TEST: booking the same dentist at the same date and time must be " +
                "refused. This is the scenario's 'double bookings' problem.");
    }

    @Test
    @Order(6)
    @DisplayName("the same time slot IS allowed for a different dentist")
    void allowsSameSlotForDifferentDentist() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        LocalTime slot = LocalTime.of(13, 30);
        String contactA = "07" + (System.currentTimeMillis() + 5) % 100000000L;
        String contactB = "07" + (System.currentTimeMillis() + 6) % 100000000L;

        Patient patientA = patientDAO.findOrCreate("Patient C", "7 Test Lane", contactA);
        Patient patientB = patientDAO.findOrCreate("Patient D", "8 Test Lane", contactB);

        Appointment withDentist1 = appointmentDAO.register(
                patientA.getPatientId(), "D0001", "T0001", TEST_DATE, slot);
        Appointment withDentist3 = appointmentDAO.register(
                patientB.getPatientId(), "D0003", "T0001", TEST_DATE, slot);

        assertNotNull(withDentist1, "First dentist should be bookable at this slot.");
        assertNotNull(withDentist3,
                "A DIFFERENT dentist must still be bookable at the same time - the clinic " +
                "has several dentists working in parallel.");
    }

    @Test
    @Order(7)
    @DisplayName("isSlotTaken() reports true only for an occupied slot")
    void reportsSlotAvailability() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        LocalTime takenSlot = LocalTime.of(14, 45);
        String contact = "07" + (System.currentTimeMillis() + 7) % 100000000L;
        Patient patient = patientDAO.findOrCreate("Slot Patient", "9 Test Lane", contact);

        assertFalse(appointmentDAO.isSlotTaken("D0001", TEST_DATE, takenSlot),
                "The slot should be free before anything is booked.");

        appointmentDAO.register(patient.getPatientId(), "D0001", "T0001", TEST_DATE, takenSlot);

        assertTrue(appointmentDAO.isSlotTaken("D0001", TEST_DATE, takenSlot),
                "The slot should report as taken once booked.");
    }

    @Test
    @Order(8)
    @DisplayName("findByNumber() retrieves an appointment with joined patient/dentist/treatment names")
    void findsAppointmentByNumber() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        String contact = "07" + (System.currentTimeMillis() + 8) % 100000000L;
        Patient patient = patientDAO.findOrCreate("Lookup Patient", "10 Test Lane", contact);
        Appointment saved = appointmentDAO.register(
                patient.getPatientId(), "D0001", "T0005", TEST_DATE, LocalTime.of(15, 30));
        assertNotNull(saved);

        Appointment found = appointmentDAO.findByNumber(saved.getAppointmentNumber());

        assertNotNull(found, "The saved appointment should be findable by its number.");
        assertEquals("Lookup Patient", found.getPatientName());
        assertEquals("Root Canal Treatment", found.getTreatmentName());
        assertEquals(25000.00, found.getTreatmentCost(), 0.01,
                "The joined treatment cost should come from the treatments table.");
    }

    @Test
    @Order(9)
    @DisplayName("findByNumber() returns null for an appointment number that does not exist")
    void returnsNullForUnknownNumber() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        assertNull(appointmentDAO.findByNumber("APT9999-9999"),
                "An unknown appointment number must return null, not throw.");
    }

    @Test
    @Order(10)
    @DisplayName("findByDate() returns the appointments booked for that day")
    void findsAppointmentsByDate() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        List<Appointment> onTestDate = appointmentDAO.findByDate(TEST_DATE);
        assertFalse(onTestDate.isEmpty(),
                "Earlier tests booked appointments on this date, so the list should not be empty.");

        for (Appointment a : onTestDate) {
            assertEquals(TEST_DATE, a.getAppointmentDate(),
                    "findByDate must only return appointments for the requested date.");
        }
    }

    @Test
    @Order(11)
    @DisplayName("cancelling a slot frees it for rebooking")
    void cancellingFreesTheSlot() throws SQLException {
        assumeTrue(databaseAvailable, "No database connection");

        LocalTime slot = LocalTime.of(16, 0);
        String contact = "07" + (System.currentTimeMillis() + 9) % 100000000L;
        Patient patient = patientDAO.findOrCreate("Cancel Patient", "11 Test Lane", contact);

        Appointment booked = appointmentDAO.register(
                patient.getPatientId(), "D0003", "T0001", TEST_DATE, slot);
        assertNotNull(booked);
        assertTrue(appointmentDAO.isSlotTaken("D0003", TEST_DATE, slot));

        appointmentDAO.updateStatus(booked.getAppointmentNumber(), Appointment.STATUS_CANCELLED);

        assertFalse(appointmentDAO.isSlotTaken("D0003", TEST_DATE, slot),
                "A cancelled appointment must not keep blocking the slot.");
    }
}
