package dao;

import db.DBConnection;
import model.Patient;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object Design Pattern) for {@link Patient}.
 * Every SQL statement for patients lives in this one class — the view
 * (JFrame) layer never writes SQL itself, it only ever calls these
 * methods and works with plain Patient objects.
 */
public class PatientDAO {

    private final DBConnection db = DBConnection.getInstance();

    public Patient findById(String patientId) throws SQLException {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Patient findByContactNumber(String contactNumber) throws SQLException {
        String sql = "SELECT * FROM patients WHERE contact_number = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, contactNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Patient> findAll() throws SQLException {
        String sql = "SELECT * FROM patients ORDER BY name";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Patient> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Patient> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM patients WHERE name LIKE ? OR contact_number LIKE ? ORDER BY name";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Patient> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    /** Inserts a brand-new patient row and fills in the generated id. */
    public Patient insert(Patient patient) throws SQLException {
        try (Connection c = db.getConnection()) {
            String id = IdGenerator.next(c, "patients", "patient_id", "P", 4);
            patient.setPatientId(id);
            String sql = "INSERT INTO patients (patient_id, name, address, contact_number) VALUES (?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, patient.getPatientId());
                ps.setString(2, patient.getName());
                ps.setString(3, patient.getAddress());
                ps.setString(4, patient.getContactNumber());
                ps.executeUpdate();
            }
            return patient;
        }
    }

    public void update(Patient patient) throws SQLException {
        String sql = "UPDATE patients SET name = ?, address = ?, contact_number = ? WHERE patient_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patient.getName());
            ps.setString(2, patient.getAddress());
            ps.setString(3, patient.getContactNumber());
            ps.setString(4, patient.getPatientId());
            ps.executeUpdate();
        }
    }

    /**
     * Finds the patient with this contact number and refreshes their
     * name/address, or inserts a brand-new patient if the number has
     * never been seen before. This is what stops the same person being
     * registered twice (Task A, Section 3.2).
     */
    public Patient findOrCreate(String name, String address, String contactNumber) throws SQLException {
        Patient existing = findByContactNumber(contactNumber);
        if (existing != null) {
            existing.setName(name);
            existing.setAddress(address);
            update(existing);
            return existing;
        }
        return insert(new Patient(null, name, address, contactNumber));
    }

    private Patient map(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getString("patient_id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("contact_number")
        );
    }
}
