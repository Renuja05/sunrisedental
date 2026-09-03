package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.PatientDAO;
import model.Patient;
import model.User;
import web.BaseHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Handles: GET /api/patients?query=keyword -> { patients: [...] } */
public class PatientHandler extends BaseHandler {

    private final PatientDAO patientDAO = new PatientDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        User user = currentUser(exchange);
        if (rejectIfNotLoggedIn(exchange, user)) return;
        if (!exchange.getRequestMethod().equals("GET")) {
            sendError(exchange, 405, "Method not allowed.");
            return;
        }
        String keyword = queryParams(exchange).get("query");
        List<Patient> patients = (keyword == null || keyword.isBlank())
                ? patientDAO.findAll() : patientDAO.search(keyword.trim());
        List<Object> maps = new ArrayList<>();
        for (Patient p : patients) maps.add(p.toMap());
        sendJson(exchange, 200, Map.of("patients", maps));
    }
}
