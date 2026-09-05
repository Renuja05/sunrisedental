package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.AppointmentDAO;
import dao.BillDAO;
import model.Appointment;
import model.Bill;
import model.User;
import util.Json;
import web.BaseHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;


public class BillHandler extends BaseHandler {

    private static final String PREFIX = "/api/bills";
    private static final double CONSULTATION_FEE = 1000.00;
    private static final double LOYALTY_DISCOUNT_RATE = 0.10;
    private static final int LOYALTY_MIN_VISITS = 3;

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final BillDAO billDAO = new BillDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        User user = currentUser(exchange);
        if (rejectIfNotLoggedIn(exchange, user)) return;

        String method = exchange.getRequestMethod();
        String[] rest = remainingPath(exchange, PREFIX);

        if (method.equals("POST") && rest.length == 0) {
            Map<String, Object> body = readJsonBody(exchange);
            String appointmentNumber = Json.getString(body, "appointmentNumber");
            Boolean applyDiscount = Json.getBoolean(body, "applyDiscount");
            if (appointmentNumber == null || appointmentNumber.isEmpty()) {
                sendError(exchange, 400, "Appointment number is required.");
                return;
            }

            Appointment appointment = appointmentDAO.findByNumber(appointmentNumber.toUpperCase());
            if (appointment == null) {
                sendError(exchange, 404, "No appointment found with number " + appointmentNumber + ".");
                return;
            }
            if (billDAO.findByAppointmentNumber(appointment.getAppointmentNumber()) != null) {
                sendError(exchange, 409, "A bill already exists for this appointment.");
                return;
            }

            int visitCount = appointmentDAO.findByPatientId(appointment.getPatientId()).size();
            double treatmentCost = appointment.getTreatmentCost();
            double discount = 0;
            if (Boolean.TRUE.equals(applyDiscount) && visitCount >= LOYALTY_MIN_VISITS) {
                discount = treatmentCost * LOYALTY_DISCOUNT_RATE;
            }

            Bill bill = billDAO.generateBill(appointment.getAppointmentNumber(), CONSULTATION_FEE, treatmentCost, discount);
            bill.setPatientName(appointment.getPatientName());
            bill.setDentistName(appointment.getDentistName());
            bill.setTreatmentName(appointment.getTreatmentName());
            sendJson(exchange, 201, bill.toMap());

        } else if (method.equals("GET") && rest.length == 1) {
            Bill bill = billDAO.findByAppointmentNumber(rest[0].toUpperCase());
            if (bill == null) {
                sendError(exchange, 404, "No bill has been generated yet for appointment " + rest[0] + ".");
                return;
            }
            sendJson(exchange, 200, bill.toMap());

        } else {
            sendError(exchange, 404, "Unknown endpoint.");
        }
    }
}
