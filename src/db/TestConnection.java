package db;

/**
 * Run this file on its own (right-click → Run File) the first time you
 * set up the project, to check MySQL is reachable before you try the
 * full application. It does not touch any table — it only opens and
 * immediately closes a connection through the {@link DBConnection}
 * Singleton.
 */
public class TestConnection {

    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        boolean ok = DBConnection.getInstance().testConnection();
        if (ok) {
            System.out.println("SUCCESS: connected to the sunrise_dental database.");
        } else {
            System.out.println("FAILED: could not connect. Check that:");
            System.out.println("  1. MySQL is running.");
            System.out.println("  2. You ran sql/schema.sql to create the sunrise_dental database.");
            System.out.println("  3. The URL/username/password in DBConnection.java are correct.");
            System.out.println("  4. mysql-connector-j-x.x.x.jar has been added to the project Libraries.");
        }
    }
}
