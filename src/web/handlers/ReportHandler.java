package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.AppointmentDAO;
import dao.BillDAO;
import model.Appointment;
import model.Bill;
import model.User;
import web.BaseHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles:
 *   GET /api/reports/daily-schedule?date=YYYY-MM-DD
 *   GET /api/reports/patient-history?patientId=Pxxxx
 *   GET /api/reports/revenue?from=YYYY-MM-DD&to=YYYY-MM-DD
 *   GET /api/reports/dentist-workload?from=YYYY-MM-DD&to=YYYY-MM-DD
 */
public class ReportHandler extends BaseHandler {

    private static final String PREFIX = "/api/reports";

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final BillDAO billDAO = new BillDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        User user = currentUser(exchange);
        if (rejectIfNotLoggedIn(exchange, user)) return;

        String[] rest = remainingPath(exchange, PREFIX);
        Map<String, String> params = queryParams(exchange);

        if (rest.length == 0 || !exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 404, "Unknown report endpoint.");
            return;
        }

        switch (rest[0]) {
            case "daily-schedule": {
                LocalDate date = parseDate(params.get("date"), LocalDate.now());
                List<Object> maps = new ArrayList<>();
                for (Appointment a : appointmentDAO.findByDate(date)) maps.add(a.toMap());
                sendJson(exchange, 200, Map.of("date", date.toString(), "appointments", maps));
                break;
            }
            case "patient-history": {
                String patientId = params.get("patientId");
                if (patientId == null || patientId.isEmpty()) {
                    sendError(exchange, 400, "patientId query parameter is required.");
                    return;
                }
                List<Object> maps = new ArrayList<>();
                for (Appointment a : appointmentDAO.findByPatientId(patientId)) maps.add(a.toMap());
                sendJson(exchange, 200, Map.of("patientId", patientId, "appointments", maps));
                break;
            }
            case "revenue": {
                LocalDate from = parseDate(params.get("from"), LocalDate.now().withDayOfMonth(1));
                LocalDate to = parseDate(params.get("to"), LocalDate.now());
                List<Bill> bills = billDAO.findByDateRange(from, to);

                double total = 0;
                Map<String, Double> byDay = new LinkedHashMap<>();
                for (Bill b : bills) {
                    total += b.getTotalAmount();
                    String day = b.getBillDate() == null ? "unknown" : b.getBillDate().toLocalDate().toString();
                    byDay.merge(day, b.getTotalAmount(), Double::sum);
                }
                List<Object> dailyBreakdown = new ArrayList<>();
                for (Map.Entry<String, Double> e : byDay.entrySet()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", e.getKey());
                    row.put("revenue", e.getValue());
                    dailyBreakdown.add(row);
                }
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("billCount", bills.size());
                response.put("totalRevenue", total);
                response.put("dailyBreakdown", dailyBreakdown);
                sendJson(exchange, 200, response);
                break;
            }
            case "dentist-workload": {
                LocalDate from = parseDate(params.get("from"), LocalDate.now().withDayOfMonth(1));
                LocalDate to = parseDate(params.get("to"), LocalDate.now());
                List<Appointment> appointments = appointmentDAO.findByDateRange(from, to);

                Map<String, int[]> counts = new LinkedHashMap<>();
                for (Appointment a : appointments) {
                    int[] c = counts.computeIfAbsent(a.getDentistName(), k -> new int[3]);
                    c[0]++;
                    if (Appointment.STATUS_COMPLETED.equals(a.getStatus())) c[1]++;
                    if (Appointment.STATUS_CANCELLED.equals(a.getStatus())) c[2]++;
                }
                List<Object> rows = new ArrayList<>();
                for (Map.Entry<String, int[]> e : counts.entrySet()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("dentistName", e.getKey());
                    row.put("totalAppointments", e.getValue()[0]);
                    row.put("completed", e.getValue()[1]);
                    row.put("cancelled", e.getValue()[2]);
                    rows.add(row);
                }
                sendJson(exchange, 200, Map.of("workload", rows));
                break;
            }
            default:
                sendError(exchange, 404, "Unknown report endpoint.");
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isEmpty()) return fallback;
        return LocalDate.parse(value);
    }
}
