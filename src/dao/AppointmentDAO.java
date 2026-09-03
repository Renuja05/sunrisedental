package dao;

import db.DBConnection;
import model.Appointment;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/** DAO for {@link Appointment} records — the core entity of the system. */
public class AppointmentDAO {

    private final DBConnection db = DBConnection.getInstance();

    /** Every read query joins the three reference tables so the caller
     *  gets human-readable names without a second lookup. */
    private static final String SELECT_JOINED =
            "SELECT a.*, p.name AS patient_name, p.contact_number AS patient_contact, " +
            "       p.address AS patient_address, " +
            "       d.name AS dentist_name, t.treatment_name AS treatment_name, t.cost AS treatment_cost " +
            "FROM appointments a " +
            "JOIN patients p ON a.patient_id = p.patient_id " +
            "JOIN dentists d ON a.dentist_id = d.dentist_id " +
            "JOIN treatments t ON a.treatment_id = t.treatment_id ";

    public Appointment findByNumber(String appointmentNumber) throws SQLException {
        String sql = SELECT_JOINED + "WHERE a.appointment_number = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, appointmentNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean isSlotTaken(String dentistId, LocalDate date, LocalTime time) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments " +
                "WHERE dentist_id = ? AND appointment_date = ? AND appointment_time = ? " +
                "AND status <> 'CANCELLED'";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dentistId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setTime(3, java.sql.Time.valueOf(time));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a new appointment. Returns the saved appointment (with its
     * generated number) on success, or {@code null} if the dentist is
     * already booked for that exact date/time — this is the check that
     * stops the double bookings described in the scenario, done here
     * (instead of a separate Service class) right before the INSERT.
     */
    public Appointment register(String patientId, String dentistId, String treatmentId,
                                 LocalDate date, LocalTime time) throws SQLException {
        if (isSlotTaken(dentistId, date, time)) {
            return null; // slot already booked — caller shows the error message
        }
        try (Connection c = db.getConnection()) {
            String prefix = "APT" + Year.now().getValue() + "-";
            String number = IdGenerator.next(c, "appointments", "appointment_number", prefix, 4);
            String sql = "INSERT INTO appointments " +
                    "(appointment_number, patient_id, dentist_id, treatment_id, appointment_date, appointment_time, status) " +
                    "VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, number);
                ps.setString(2, patientId);
                ps.setString(3, dentistId);
                ps.setString(4, treatmentId);
                ps.setDate(5, java.sql.Date.valueOf(date));
                ps.setTime(6, java.sql.Time.valueOf(time));
                ps.setString(7, Appointment.STATUS_SCHEDULED);
                ps.executeUpdate();
            }
            return findByNumber(number);
        }
    }

    public void updateStatus(String appointmentNumber, String status) throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE appointment_number = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, appointmentNumber);
            ps.executeUpdate();
        }
    }

    public List<Appointment> findByDate(LocalDate date) throws SQLException {
        String sql = SELECT_JOINED + "WHERE a.appointment_date = ? ORDER BY a.appointment_time";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<Appointment> findByPatientId(String patientId) throws SQLException {
        String sql = SELECT_JOINED + "WHERE a.patient_id = ? ORDER BY a.appointment_date DESC, a.appointment_time DESC";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<Appointment> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = SELECT_JOINED + "WHERE a.appointment_date BETWEEN ? AND ? " +
                "ORDER BY a.appointment_date, a.appointment_time";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<Appointment> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment a = new Appointment(
                rs.getString("appointment_number"),
                rs.getString("patient_id"),
                rs.getString("dentist_id"),
                rs.getString("treatment_id"),
                rs.getDate("appointment_date").toLocalDate(),
                rs.getTime("appointment_time").toLocalTime(),
                rs.getString("status")
        );
        a.setPatientName(rs.getString("patient_name"));
        a.setPatientContact(rs.getString("patient_contact"));
        a.setPatientAddress(rs.getString("patient_address"));
        a.setDentistName(rs.getString("dentist_name"));
        a.setTreatmentName(rs.getString("treatment_name"));
        a.setTreatmentCost(rs.getDouble("treatment_cost"));
        return a;
    }
}
