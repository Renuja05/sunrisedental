package dao;

import db.DBConnection;
import model.Treatment;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class TreatmentDAO {

    private final DBConnection db = DBConnection.getInstance();

    public List<Treatment> findAll() throws SQLException {
        String sql = "SELECT * FROM treatments ORDER BY treatment_name";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Treatment> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public Treatment findById(String treatmentId) throws SQLException {
        String sql = "SELECT * FROM treatments WHERE treatment_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, treatmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Treatment insert(Treatment treatment) throws SQLException {
        try (Connection c = db.getConnection()) {
            String id = IdGenerator.next(c, "treatments", "treatment_id", "T", 4);
            treatment.setTreatmentId(id);
            String sql = "INSERT INTO treatments (treatment_id, treatment_name, cost) VALUES (?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, treatment.getTreatmentId());
                ps.setString(2, treatment.getTreatmentName());
                ps.setDouble(3, treatment.getCost());
                ps.executeUpdate();
            }
            return treatment;
        }
    }

    private Treatment map(ResultSet rs) throws SQLException {
        return new Treatment(
                rs.getString("treatment_id"),
                rs.getString("treatment_name"),
                rs.getDouble("cost")
        );
    }
}
