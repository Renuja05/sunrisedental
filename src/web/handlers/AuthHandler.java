package web.handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.UserDAO;
import model.User;
import util.Json;
import util.PasswordUtil;
import util.Validator;
import web.BaseHandler;
import web.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles:
 *   POST /api/login  { username, password } -> { token, user }
 *   POST /api/logout                          (X-Auth-Token header)
 */
public class AuthHandler extends BaseHandler {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doHandle(HttpExchange exchange) throws IOException, SQLException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/login") && method.equals("POST")) {
            Map<String, Object> body = readJsonBody(exchange);
            String username = Json.getString(body, "username");
            String password = Json.getString(body, "password");

            String err = Validator.requiredText(username, "Username");
            if (err == null) err = Validator.requiredText(password, "Password");
            if (err != null) { sendError(exchange, 400, err); return; }

            User user = userDAO.findByUsername(username.trim());
            if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
                sendError(exchange, 401, "Invalid username or password.");
                return;
            }
            if (!user.isActive()) {
                sendError(exchange, 401, "This account has been deactivated. Please contact an administrator.");
                return;
            }

            String token = SessionManager.getInstance().createSession(user);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("token", token);
            response.put("user", user.toMap());
            sendJson(exchange, 200, response);

        } else if (path.equals("/api/logout") && method.equals("POST")) {
            String token = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            SessionManager.getInstance().invalidate(token);
            sendJson(exchange, 200, Map.of("message", "Logged out."));

        } else {
            sendError(exchange, 405, "Method not allowed.");
        }
    }
}
