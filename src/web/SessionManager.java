package web;

import model.User;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of which opaque session token belongs to which logged-in
 * {@link User}. The browser receives a token from {@code POST
 * /api/login} and stores it (in sessionStorage) so it can send it back
 * in the {@code X-Auth-Token} header on every later request — the same
 * pattern real REST APIs use, kept deliberately simple (in-memory, no
 * expiry) since this server runs as a single process on the clinic's
 * own network.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final ConcurrentHashMap<String, User> sessions = new ConcurrentHashMap<>();

    private SessionManager() { }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public User getUser(String token) {
        return token == null ? null : sessions.get(token);
    }

    public void invalidate(String token) {
        if (token != null) sessions.remove(token);
    }
}
