package dao;

import db.DBConnection;
import model.Bill;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/** DAO for {@link Bill} records. */
public class BillDAO {

    private final DBConnection db = DBConnection.getInstance();

    private static final String SELECT_JOINED =
            "SELECT b.*, p.name AS patient_name, d.name AS dentist_name, t.treatment_name AS treatment_name " +
            "FROM bills b " +
            "JOIN appointments a ON b.appointment_number = a.appointment_number " +
            "JOIN patients p ON a.patient_id = p.patient_id " +
            "JOIN dentists d ON a.dentist_id = d.dentist_id " +
            "JOIN treatments t ON a.treatment_id = t.treatment_id ";

    public Bill findByAppointmentNumber(String appointmentNumber) throws SQLException {
        String sql = SELECT_JOINED + "WHERE b.appointment_number = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, appointmentNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Calculates and stores the bill for one appointment: total =
     * consultation fee + treatment cost − discount. Also marks the
     * appointment as COMPLETED. Returns {@code null} if a bill already
     * exists for this appointment (each appointment is only billed once).
     */
    public Bill generateBill(String appointmentNumber, double consultationFee,
                              double treatmentCost, double discount) throws SQLException {
        if (findByAppointmentNumber(appointmentNumber) != null) {
            return null; // already billed
        }
        double total = consultationFee + treatmentCost - discount;
        try (Connection c = db.getConnection()) {
            String prefix = "BILL" + Year.now().getValue() + "-";
            String number = IdGenerator.next(c, "bills", "bill_number", prefix, 4);
            String sql = "INSERT INTO bills " +
                    "(bill_number, appointment_number, consultation_fee, treatment_cost, discount, total_amount) " +
                    "VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, number);
                ps.setString(2, appointmentNumber);
                ps.setDouble(3, consultationFee);
                ps.setDouble(4, treatmentCost);
                ps.setDouble(5, discount);
                ps.setDouble(6, total);
                ps.executeUpdate();
            }
            String updateAppt = "UPDATE appointments SET status = 'COMPLETED' WHERE appointment_number = ?";
            try (PreparedStatement ps = c.prepareStatement(updateAppt)) {
                ps.setString(1, appointmentNumber);
                ps.executeUpdate();
            }
            return findByAppointmentNumber(appointmentNumber);
        }
    }

    public List<Bill> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = SELECT_JOINED + "WHERE DATE(b.bill_date) BETWEEN ? AND ? ORDER BY b.bill_date";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<Bill> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill(
                rs.getString("bill_number"),
                rs.getString("appointment_number"),
                rs.getDouble("consultation_fee"),
                rs.getDouble("treatment_cost"),
                rs.getDouble("discount"),
                rs.getDouble("total_amount")
        );
        java.sql.Timestamp ts = rs.getTimestamp("bill_date");
        if (ts != null) b.setBillDate(ts.toLocalDateTime());
        b.setPatientName(rs.getString("patient_name"));
        b.setDentistName(rs.getString("dentist_name"));
        b.setTreatmentName(rs.getString("treatment_name"));
        return b;
    }
}
