package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.DentistDAO;
import dao.TreatmentDAO;
import model.Dentist;
import model.Treatment;
import model.User;
import util.Json;
import util.Validator;
import web.BaseHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ReferenceDataHandler extends BaseHandler {

    private final DentistDAO dentistDAO = new DentistDAO();
    private final TreatmentDAO treatmentDAO = new TreatmentDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        User user = currentUser(exchange);
        if (rejectIfNotLoggedIn(exchange, user)) return;

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/dentists")) {
            if (method.equals("GET")) {
                List<Object> maps = new ArrayList<>();
                for (Dentist d : dentistDAO.findAll()) maps.add(d.toMap());
                sendJson(exchange, 200, Map.of("dentists", maps));
            } else if (method.equals("POST")) {
                if (rejectIfNotAdmin(exchange, user)) return;
                Map<String, Object> body = readJsonBody(exchange);
                String name = Json.getString(body, "name");
                String specialization = Json.getString(body, "specialization");
                String err = Validator.fullName(name);
                if (err == null) err = Validator.requiredText(specialization, "Specialization");
                if (err != null) { sendError(exchange, 400, err); return; }
                Dentist saved = dentistDAO.insert(new Dentist(null, name.trim(), specialization.trim(), true));
                sendJson(exchange, 201, saved.toMap());
            } else {
                sendError(exchange, 405, "Method not allowed.");
            }

        } else if (path.equals("/api/treatments")) {
            if (method.equals("GET")) {
                List<Object> maps = new ArrayList<>();
                for (Treatment t : treatmentDAO.findAll()) maps.add(t.toMap());
                sendJson(exchange, 200, Map.of("treatments", maps));
            } else if (method.equals("POST")) {
                if (rejectIfNotAdmin(exchange, user)) return;
                Map<String, Object> body = readJsonBody(exchange);
                String name = Json.getString(body, "treatmentName");
                Double cost = Json.getDouble(body, "cost");
                String err = Validator.requiredText(name, "Treatment name");
                if (err == null && (cost == null || cost <= 0)) err = "Cost must be a positive number.";
                if (err != null) { sendError(exchange, 400, err); return; }
                Treatment saved = treatmentDAO.insert(new Treatment(null, name.trim(), cost));
                sendJson(exchange, 201, saved.toMap());
            } else {
                sendError(exchange, 405, "Method not allowed.");
            }

        } else {
            sendError(exchange, 404, "Unknown endpoint.");
        }
    }
}
