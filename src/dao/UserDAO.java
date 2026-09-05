package dao;

import db.DBConnection;
import model.Administrator;
import model.Receptionist;
import model.User;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class UserDAO {

    private final DBConnection db = DBConnection.getInstance();

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public User insert(String username, String passwordHash, String fullName, String role) throws SQLException {
        try (Connection c = db.getConnection()) {
            String id = IdGenerator.next(c, "users", "user_id", "U", 4);
            String sql = "INSERT INTO users (user_id, username, password_hash, full_name, role, active) " +
                    "VALUES (?,?,?,?,?,TRUE)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, username);
                ps.setString(3, passwordHash);
                ps.setString(4, fullName);
                ps.setString(5, role);
                ps.executeUpdate();
            }
            return role.equals("ADMINISTRATOR")
                    ? new Administrator(id, username, passwordHash, fullName, true)
                    : new Receptionist(id, username, passwordHash, fullName, true);
        }
    }

    public void setActive(String userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public void resetPassword(String userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    
    private User map(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String id = rs.getString("user_id");
        String username = rs.getString("username");
        String hash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        boolean active = rs.getBoolean("active");

        if ("ADMINISTRATOR".equals(role)) {
            return new Administrator(id, username, hash, fullName, active);
        }
        return new Receptionist(id, username, hash, fullName, active);
    }
}
