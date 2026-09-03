package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.UserDAO;
import model.User;
import util.Json;
import util.PasswordUtil;
import util.Validator;
import web.BaseHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Staff-account administration — every operation here requires an
 * Administrator session.
 *
 *   GET  /api/staff                     -> list all accounts
 *   POST /api/staff                     { username, password, fullName, role } -> create account
 *   PUT  /api/staff/{userId}/active     { active: true|false }
 *   PUT  /api/staff/{userId}/password   { password }
 */
public class StaffHandler extends BaseHandler {

    private static final String PREFIX = "/api/staff";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        User user = currentUser(exchange);
        if (rejectIfNotAdmin(exchange, user)) return;

        String method = exchange.getRequestMethod();
        String[] rest = remainingPath(exchange, PREFIX);

        if (method.equals("GET") && rest.length == 0) {
            List<Object> maps = new ArrayList<>();
            for (User u : userDAO.findAll()) maps.add(u.toMap());
            sendJson(exchange, 200, Map.of("staff", maps));

        } else if (method.equals("POST") && rest.length == 0) {
            Map<String, Object> body = readJsonBody(exchange);
            String username = Json.getString(body, "username");
            String password = Json.getString(body, "password");
            String fullName = Json.getString(body, "fullName");
            String role = Json.getString(body, "role");

            String err = Validator.username(username);
            if (err == null) err = Validator.password(password);
            if (err == null) err = Validator.fullName(fullName);
            if (err == null && (role == null || (!role.equals("RECEPTIONIST") && !role.equals("ADMINISTRATOR")))) {
                err = "Role must be RECEPTIONIST or ADMINISTRATOR.";
            }
            if (err != null) { sendError(exchange, 400, err); return; }
            if (userDAO.findByUsername(username.trim()) != null) {
                sendError(exchange, 400, "That username is already taken.");
                return;
            }

            User created = userDAO.insert(username.trim(), PasswordUtil.hash(password), fullName.trim(), role);
            sendJson(exchange, 201, created.toMap());

        } else if (method.equals("PUT") && rest.length == 2 && rest[1].equals("active")) {
            Map<String, Object> body = readJsonBody(exchange);
            Boolean active = Json.getBoolean(body, "active");
            if (active == null) { sendError(exchange, 400, "'active' (true/false) is required."); return; }
            userDAO.setActive(rest[0], active);
            sendJson(exchange, 200, Map.of("message", "Account updated."));

        } else if (method.equals("PUT") && rest.length == 2 && rest[1].equals("password")) {
            Map<String, Object> body = readJsonBody(exchange);
            String newPassword = Json.getString(body, "password");
            String err = Validator.password(newPassword);
            if (err != null) { sendError(exchange, 400, err); return; }
            userDAO.resetPassword(rest[0], PasswordUtil.hash(newPassword));
            sendJson(exchange, 200, Map.of("message", "Password reset."));

        } else {
            sendError(exchange, 404, "Unknown endpoint.");
        }
    }
}
