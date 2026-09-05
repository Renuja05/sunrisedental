package dao;

import db.DBConnection;
import model.Appointment;
import model.Patient;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * INTEGRATION tests for the DAO layer (JUnit 4).
 *
 * Unlike the tests in util and model, these need a running MySQL server
 * with the sunrise_dental database created from sql/schema.sql. Rather
 * than failing when the database is absent, every test calls
 * assumeTrue(...) first, which SKIPS the test instead - so the suite
 * still runs cleanly on a machine without a database, and only the
 * unit tests execute.
 *
 * The single most important test here is test05_rejectsDoubleBooking():
 * it proves the fix for the exact problem the scenario opens with
 * ("This has resulted in double bookings").
 *
 * Method names are prefixed test01_, test02_ ... because
 * @FixMethodOrder(NAME_ASCENDING) orders by name - later tests
 * legitimately build on data created by earlier ones.
 *
 * NOTE: these tests write real rows into the database. Run
 * sql/schema.sql again afterwards to reset it to clean seed data.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppointmentDAOIntegrationTest {

    private static boolean databaseAvailable;

    private final PatientDAO patientDAO = new PatientDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final DentistDAO dentistDAO = new DentistDAO();
    private final TreatmentDAO treatmentDAO = new TreatmentDAO();

    /** A date far enough ahead that it will not clash with seed or demo data. */
    private static final LocalDate TEST_DATE = LocalDate.now().plusDays(30);

    @BeforeClass
    public static void checkDatabase() {
        databaseAvailable = DBConnection.getInstance().testConnection();
        if (!databaseAvailable) {
            System.out.println("SKIPPING DAO integration tests: no database connection. " +
                    "Start MySQL and run sql/schema.sql to enable them.");
        }
    }

    /** Unique-ish contact number so repeated runs do not collide. */
    private String uniqueContact(int offset) {
        long n = (System.currentTimeMillis() + offset) % 100000000L;
        return "07" + String.format("%08d", n);
    }

    @Test
    public void test01_seedDataIsPresent() throws SQLException {
        assumeTrue(databaseAvailable);

        assertEquals("schema.sql should have inserted exactly 3 dentists.",
                3, dentistDAO.findAll().size());
        assertEquals("schema.sql should have inserted exactly 6 treatment types.",
                6, treatmentDAO.findAll().size());
    }

    @Test
    public void test02_createsNewPatient() throws SQLException {
        assumeTrue(databaseAvailable);

        Patient patient = patientDAO.findOrCreate(
                "Test Patient", "1 Test Lane, Colombo", uniqueContact(0));

        assertNotNull("A generated patient id should have been assigned.", patient.getPatientId());
        assertTrue("Patient ids should start with P.", patient.getPatientId().startsWith("P"));
        assertEquals("Test Patient", patient.getName());
    }

    @Test
    public void test03_reusesExistingPatient() throws SQLException {
        assumeTrue(databaseAvailable);

        String contact = uniqueContact(1);
        Patient first = patientDAO.findOrCreate("Repeat Patient", "2 Test Lane", contact);
        Patient second = patientDAO.findOrCreate("Repeat Patient Updated", "3 New Lane", contact);

        assertEquals("The same contact number must map to one patient record, not two. " +
                     "This is the fix for the scenario's 'lost patient records' problem.",
                first.getPatientId(), second.getPatientId());
        assertEquals("Re-registering should refresh the stored name.",
                "Repeat Patient Updated", second.getName());
    }

    @Test
    public void test04_registersAppointment() throws SQLException {
        assumeTrue(databaseAvailable);

        Patient patient = patientDAO.findOrCreate("Booking Patient", "4 Test Lane", uniqueContact(2));
        Appointment appointment = appointmentDAO.register(
                patient.getPatientId(), "D0001", "T0001", TEST_DATE, LocalTime.of(9, 0));

        assertNotNull("A free slot should produce a saved appointment, not null.", appointment);
        assertTrue("Appointment numbers should start with APT.",
                appointment.getAppointmentNumber().startsWith("APT"));
        assertEquals(Appointment.STATUS_SCHEDULED, appointment.getStatus());
        assertEquals("The joined query should populate the dentist's name.",
                "Dr. S. Fernando", appointment.getDentistName());
    }

    @Test
    public void test05_rejectsDoubleBooking() throws SQLException {
        assumeTrue(databaseAvailable);

        LocalTime slot = LocalTime.of(11, 15);
        Patient patientA = patientDAO.findOrCreate("Patient A", "5 Test Lane", uniqueContact(3));
        Patient patientB = patientDAO.findOrCreate("Patient B", "6 Test Lane", uniqueContact(4));

        Appointment first = appointmentDAO.register(
                patientA.getPatientId(), "D0002", "T0002", TEST_DATE, slot);
        assertNotNull("The first booking of a free slot must succeed.", first);

        Appointment second = appointmentDAO.register(
                patientB.getPatientId(), "D0002", "T0003", TEST_DATE, slot);

        assertNull("THE KEY TEST: booking the same dentist at the same date and time must be " +
                   "refused. This is the scenario's 'double bookings' problem.", second);
    }

    @Test
    public void test06_allowsSameSlotForDifferentDentist() throws SQLException {
        assumeTrue(databaseAvailable);

        LocalTime slot = LocalTime.of(13, 30);
        Patient patientA = patientDAO.findOrCreate("Patient C", "7 Test Lane", uniqueContact(5));
        Patient patientB = patientDAO.findOrCreate("Patient D", "8 Test Lane", uniqueContact(6));

        Appointment withDentist1 = appointmentDAO.register(
                patientA.getPatientId(), "D0001", "T0001", TEST_DATE, slot);
        Appointment withDentist3 = appointmentDAO.register(
                patientB.getPatientId(), "D0003", "T0001", TEST_DATE, slot);

        assertNotNull("First dentist should be bookable at this slot.", withDentist1);
        assertNotNull("A DIFFERENT dentist must still be bookable at the same time - the clinic " +
                      "has several dentists working in parallel.", withDentist3);
    }

    @Test
    public void test07_reportsSlotAvailability() throws SQLException {
        assumeTrue(databaseAvailable);

        LocalTime takenSlot = LocalTime.of(14, 45);
        Patient patient = patientDAO.findOrCreate("Slot Patient", "9 Test Lane", uniqueContact(7));

        assertFalse("The slot should be free before anything is booked.",
                appointmentDAO.isSlotTaken("D0001", TEST_DATE, takenSlot));

        appointmentDAO.register(patient.getPatientId(), "D0001", "T0001", TEST_DATE, takenSlot);

        assertTrue("The slot should report as taken once booked.",
                appointmentDAO.isSlotTaken("D0001", TEST_DATE, takenSlot));
    }

    @Test
    public void test08_findsAppointmentByNumber() throws SQLException {
        assumeTrue(databaseAvailable);

        Patient patient = patientDAO.findOrCreate("Lookup Patient", "10 Test Lane", uniqueContact(8));
        Appointment saved = appointmentDAO.register(
                patient.getPatientId(), "D0001", "T0005", TEST_DATE, LocalTime.of(15, 30));
        assertNotNull(saved);

        Appointment found = appointmentDAO.findByNumber(saved.getAppointmentNumber());

        assertNotNull("The saved appointment should be findable by its number.", found);
        assertEquals("Lookup Patient", found.getPatientName());
        assertEquals("Root Canal Treatment", found.getTreatmentName());
        assertEquals("The joined treatment cost should come from the treatments table.",
                25000.00, found.getTreatmentCost().doubleValue(), 0.01);
    }

    @Test
    public void test09_returnsNullForUnknownNumber() throws SQLException {
        assumeTrue(databaseAvailable);

        assertNull("An unknown appointment number must return null, not throw.",
                appointmentDAO.findByNumber("APT9999-9999"));
    }

    @Test
    public void test10_findsAppointmentsByDate() throws SQLException {
        assumeTrue(databaseAvailable);

        List<Appointment> onTestDate = appointmentDAO.findByDate(TEST_DATE);
        assertFalse("Earlier tests booked appointments on this date, so the list should not be empty.",
                onTestDate.isEmpty());

        for (Appointment a : onTestDate) {
            assertEquals("findByDate must only return appointments for the requested date.",
                    TEST_DATE, a.getAppointmentDate());
        }
    }

    @Test
    public void test11_cancellingFreesTheSlot() throws SQLException {
        assumeTrue(databaseAvailable);

        LocalTime slot = LocalTime.of(16, 0);
        Patient patient = patientDAO.findOrCreate("Cancel Patient", "11 Test Lane", uniqueContact(9));

        Appointment booked = appointmentDAO.register(
                patient.getPatientId(), "D0003", "T0001", TEST_DATE, slot);
        assertNotNull(booked);
        assertTrue(appointmentDAO.isSlotTaken("D0003", TEST_DATE, slot));

        appointmentDAO.updateStatus(booked.getAppointmentNumber(), Appointment.STATUS_CANCELLED);

        assertFalse("A cancelled appointment must not keep blocking the slot.",
                appointmentDAO.isSlotTaken("D0003", TEST_DATE, slot));
    }
}
