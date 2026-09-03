package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.User;
import util.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared plumbing for every REST endpoint: reading a JSON request body,
 * writing a JSON response, reading query parameters, and checking the
 * X-Auth-Token header. Any SQLException or unexpected error is turned
 * into a clean JSON error response instead of a stack trace reaching
 * the browser.
 */
public abstract class BaseHandler implements HttpHandler {

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            doHandle(exchange);
        } catch (SQLException e) {
            System.err.println("Database error handling " + exchange.getRequestURI() + ": " + e.getMessage());
            sendError(exchange, 500, "A database error occurred. Please try again later.");
        } catch (Exception e) {
            System.err.println("Unexpected error handling " + exchange.getRequestURI() + ": " + e);
            e.printStackTrace();
            sendError(exchange, 500, "An unexpected server error occurred.");
        } finally {
            exchange.close();
        }
    }

    protected abstract void doHandle(HttpExchange exchange) throws IOException, SQLException;

    // ---------------------------------------------------------------
    // Request helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = is.read(chunk)) != -1) buffer.write(chunk, 0, read);
        String body = buffer.toString(StandardCharsets.UTF_8);
        if (body.isEmpty()) return new LinkedHashMap<>();
        Object parsed = Json.parse(body);
        return parsed instanceof Map ? (Map<String, Object>) parsed : new LinkedHashMap<>();
    }

    protected Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String key = java.net.URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = java.net.URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    /** Path segments after the given prefix, e.g. for prefix "/api/appointments"
     *  and path "/api/appointments/APT2026-0001" this returns ["APT2026-0001"]. */
    protected String[] remainingPath(HttpExchange exchange, String prefix) {
        String path = exchange.getRequestURI().getPath();
        String rest = path.length() > prefix.length() ? path.substring(prefix.length()) : "";
        while (rest.startsWith("/")) rest = rest.substring(1);
        if (rest.isEmpty()) return new String[0];
        return rest.split("/");
    }

    /** Returns the logged-in user for this request, or null if there isn't one. */
    protected User currentUser(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("X-Auth-Token");
        return SessionManager.getInstance().getUser(token);
    }

    // ---------------------------------------------------------------
    // Response helpers
    // ---------------------------------------------------------------

    protected void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] bytes = Json.write(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        sendJson(exchange, statusCode, body);
    }

    /** True if there's no logged-in user; also writes the 401 response. Call
     *  this first in doHandle() and "return;" immediately if it returns true. */
    protected boolean rejectIfNotLoggedIn(HttpExchange exchange, User user) throws IOException {
        if (user == null) {
            sendError(exchange, 401, "You must be logged in to perform this action.");
            return true;
        }
        return false;
    }

    /** True if the user isn't an Administrator; also writes the 401 response. */
    protected boolean rejectIfNotAdmin(HttpExchange exchange, User user) throws IOException {
        if (rejectIfNotLoggedIn(exchange, user)) return true;
        if (!user.canManageStaff()) {
            sendError(exchange, 401, "Only an Administrator can perform this action.");
            return true;
        }
        return false;
    }
}
