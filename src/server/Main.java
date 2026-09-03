package server;

import com.sun.net.httpserver.HttpServer;
import db.DBConnection;
import web.handlers.AppointmentHandler;
import web.handlers.AuthHandler;
import web.handlers.BillHandler;
import web.handlers.PatientHandler;
import web.handlers.ReferenceDataHandler;
import web.handlers.ReportHandler;
import web.handlers.StaffHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * The single entry point for the whole application. Run this file
 * (right-click -> Run File) — it starts one HTTP server that:
 *   1. Serves the REST API under /api/... (see web.handlers.*), and
 *   2. Serves the HTML/CSS/JS frontend out of the webapp/ folder.
 *
 * Then open a browser to http://localhost:8080/
 */
public class Main {

    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println(" Sunrise Dental Clinic - Appointment & Patient");
        System.out.println(" Management System (Web version)");
        System.out.println("=================================================");

        System.out.println("Checking database connection...");
        if (!DBConnection.getInstance().testConnection()) {
            System.err.println();
            System.err.println("Could not connect to the database. Check that:");
            System.err.println("  1. MySQL (e.g. via WAMP) is running.");
            System.err.println("  2. You ran sql/schema.sql to create the sunrise_dental database.");
            System.err.println("  3. The URL/username/password in db/DBConnection.java are correct.");
            System.err.println("  4. mysql-connector-j-*.jar has been added to the project's Libraries.");
            System.err.println();
            System.err.println("The server will still start, but every request will fail");
            System.err.println("until the database is reachable.");
        } else {
            System.out.println("Database connection OK.");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ----- REST API -----
        server.createContext("/api/login", new AuthHandler());
        server.createContext("/api/logout", new AuthHandler());
        server.createContext("/api/dentists", new ReferenceDataHandler());
        server.createContext("/api/treatments", new ReferenceDataHandler());
        server.createContext("/api/patients", new PatientHandler());
        server.createContext("/api/appointments", new AppointmentHandler());
        server.createContext("/api/bills", new BillHandler());
        server.createContext("/api/reports", new ReportHandler());
        server.createContext("/api/staff", new StaffHandler());

        // ----- Frontend static files -----
        // "webapp" sits next to "src" in the project folder.
        File webRoot = new File("webapp").getAbsoluteFile();
        if (!webRoot.exists()) {
            // Fallback for when the working directory is different (e.g. some IDE run configs)
            webRoot = new File("../webapp").getAbsoluteFile();
        }
        System.out.println("Serving frontend files from: " + webRoot);
        server.createContext("/", new StaticFileHandler(webRoot));

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Server running: open http://localhost:" + PORT + "/ in your browser.");
        System.out.println("Leave this window open while using the system.");
    }
}
