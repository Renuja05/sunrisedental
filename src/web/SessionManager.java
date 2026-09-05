package web;

import model.User;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


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
