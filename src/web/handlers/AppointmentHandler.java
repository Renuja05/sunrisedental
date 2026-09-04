package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.AppointmentDAO;
import dao.PatientDAO;
import model.Appointment;
import model.Patient;
import model.User;
import util.Json;
import util.Validator;
import web.BaseHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles:
 *   POST /api/appointments                  -> register a new appointment
 *   GET  /api/appointments/{apptNo}         -> look up one appointment
 *   GET  /api/appointments?date=YYYY-MM-DD  -> the day's schedule
 *   PUT  /api/appointments/{apptNo}/cancel  -> cancel an appointment
 */
public class AppointmentHandler extends BaseHandler {

    private static final String PREFIX = "/api/appointments";

    private final PatientDAO patientDAO = new PatientDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        User user = currentUser(exchange);
        if (rejectIfNotLoggedIn(exchange, user)) return;

        String method = exchange.getRequestMethod();
        String[] rest = remainingPath(exchange, PREFIX);

        if (method.equals("POST") && rest.length == 0) {
            Map<String, Object> body = readJsonBody(exchange);
            String patientName = Json.getString(body, "patientName");
            String address = Json.getString(body, "address");
            String contactNumber = Json.getString(body, "contactNumber");
            String dentistId = Json.getString(body, "dentistId");
            String treatmentId = Json.getString(body, "treatmentId");
            String dateStr = Json.getString(body, "date");
            String timeStr = Json.getString(body, "time");

            String err = Validator.patientName(patientName);
            if (err == null) err = Validator.address(address);
            if (err == null) err = Validator.contactNumber(contactNumber);
            if (err == null) err = Validator.selection(dentistId, "Dentist");
            if (err == null) err = Validator.selection(treatmentId, "Treatment type");
            if (err != null) { sendError(exchange, 400, err); return; }

            LocalDate date;
            LocalTime time;
            try {
                date = LocalDate.parse(dateStr);
                time = LocalTime.parse(timeStr);
            } catch (Exception e) {
                sendError(exchange, 400, "Date/time format was not understood.");
                return;
            }
            err = Validator.appointmentDate(date);
            if (err == null) err = Validator.appointmentTime(time);
            if (err != null) { sendError(exchange, 400, err); return; }

            Patient patient = patientDAO.findOrCreate(patientName.trim(), address.trim(), contactNumber.trim());
            Appointment appointment = appointmentDAO.register(
                    patient.getPatientId(), dentistId, treatmentId, date, time);

            if (appointment == null) {
                sendError(exchange, 409, "That dentist already has an appointment at " + time +
                        " on " + date + ". Please choose a different time slot.");
                return;
            }
            sendJson(exchange, 201, appointment.toMap());

        } else if (method.equals("GET") && rest.length == 1) {
            Appointment appointment = appointmentDAO.findByNumber(rest[0].toUpperCase());
            if (appointment == null) {
                sendError(exchange, 404, "No appointment found with number " + rest[0] + ".");
                return;
            }
            sendJson(exchange, 200, appointment.toMap());

        } else if (method.equals("GET") && rest.length == 0) {
            Map<String, String> params = queryParams(exchange);
            String allParam = params.get("all");
            String dateParam = params.get("date");

            List<Appointment> found;
            if ("true".equalsIgnoreCase(allParam)) {
                // "Show all appointments" - every appointment, newest first
                found = appointmentDAO.findAll();
            } else {
                LocalDate date = (dateParam == null || dateParam.isBlank())
                        ? LocalDate.now() : LocalDate.parse(dateParam);
                found = appointmentDAO.findByDate(date);
            }

            List<Object> maps = new ArrayList<>();
            for (Appointment a : found) maps.add(a.toMap());
            sendJson(exchange, 200, Map.of("appointments", maps));

        } else if (method.equals("PUT") && rest.length == 2 && rest[1].equals("cancel")) {
            appointmentDAO.updateStatus(rest[0].toUpperCase(), Appointment.STATUS_CANCELLED);
            sendJson(exchange, 200, Map.of("message", "Appointment cancelled."));

        } else {
            sendError(exchange, 404, "Unknown endpoint.");
        }
    }
}
