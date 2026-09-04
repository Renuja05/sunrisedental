package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Generates the next sequential id for a given prefix by looking at the
 * highest existing id in the table that starts with that prefix — so
 * appointment numbers, bill numbers, patient ids etc. are produced
 * automatically by the system instead of being typed in by staff.
 */
public final class IdGenerator {

    private IdGenerator() { }

    public static String next(Connection conn, String table, String idColumn,
                               String prefix, int digits) throws SQLException {
        String sql = "SELECT " + idColumn + " FROM " + table +
                " WHERE " + idColumn + " LIKE ? ORDER BY " + idColumn + " DESC LIMIT 1";
        int nextNumber = 1;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String existing = rs.getString(1);
                    String suffix = existing.substring(prefix.length());
                    try {
                        nextNumber = Integer.parseInt(suffix) + 1;
                    } catch (NumberFormatException e) {
                        nextNumber = 1;
                    }
                }
            }
        }
        String padded = String.format("%0" + digits + "d", nextNumber);
        return prefix + padded;
    }
}
