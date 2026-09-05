package dao;

import db.DBConnection;
import model.Dentist;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DentistDAO {

    private final DBConnection db = DBConnection.getInstance();

    public List<Dentist> findAll() throws SQLException {
        String sql = "SELECT * FROM dentists WHERE active = TRUE ORDER BY name";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Dentist> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public Dentist findById(String dentistId) throws SQLException {
        String sql = "SELECT * FROM dentists WHERE dentist_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dentistId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Dentist insert(Dentist dentist) throws SQLException {
        try (Connection c = db.getConnection()) {
            String id = IdGenerator.next(c, "dentists", "dentist_id", "D", 4);
            dentist.setDentistId(id);
            String sql = "INSERT INTO dentists (dentist_id, name, specialization, active) VALUES (?,?,?,TRUE)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, dentist.getDentistId());
                ps.setString(2, dentist.getName());
                ps.setString(3, dentist.getSpecialization());
                ps.executeUpdate();
            }
            return dentist;
        }
    }

    private Dentist map(ResultSet rs) throws SQLException {
        return new Dentist(
                rs.getString("dentist_id"),
                rs.getString("name"),
                rs.getString("specialization"),
                rs.getBoolean("active")
        );
    }
}
