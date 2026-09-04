package db;


public class TestConnection {

    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        boolean ok = DBConnection.getInstance().testConnection();
        if (ok) {
            System.out.println("SUCCESS: connected to the sunrise_dental database.");
        } else {
            System.out.println("FAILED: could not connect. Check that:");
            
        }
    }
}
