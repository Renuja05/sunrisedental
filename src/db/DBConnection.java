package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton (GoF Creational Design Pattern).
 *
 * There is exactly ONE instance of this class for the whole application
 * — see the private constructor and {@link #getInstance()}. It is the
 * single place where the JDBC URL, username and password are
 * configured, instead of every DAO connecting to the database in its
 * own way. Every DAO class calls {@code DBConnection.getInstance()} to
 * reach this same shared object before running any SQL.
 */
public final class DBConnection {

    // The one and only instance, created once when the class is first used.
    private static final DBConnection INSTANCE = new DBConnection();

    // ----- Connection settings: edit these to match your MySQL setup -----
    private static final String URL =
            "jdbc:mysql://localhost:3306/sunrise_dental?useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    // -----------------------------------------------------------------------

    // Private constructor: nobody outside this class can write
    // "new DBConnection()" — that is what makes this a Singleton.
    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Connector/J was not found on the classpath.");
            System.err.println("Add mysql-connector-j-x.x.x.jar to the project's Libraries.");
        }
    }

    /** The single shared access point every DAO uses to reach this class. */
    public static DBConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /** Quick check so a bad DB configuration fails fast with a clear message. */
    public boolean testConnection() {
        try (Connection c = getConnection()) {
            return c.isValid(3);
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}
