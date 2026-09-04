package model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for every system account. Concrete roles ({@link Receptionist},
 * {@link Administrator}) extend this class — this inheritance is what
 * lets a handler show the right dashboard title and decide whether
 * "Manage Staff Accounts" is allowed, without an if/else on role
 * strings anywhere else in the code.
 */
public abstract class User {

    private String userId;
    private String username;
    private String passwordHash; // SHA-256 hex digest, never sent to the browser
    private String fullName;
    private boolean active;

    protected User(String userId, String username, String passwordHash,
                    String fullName, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setActive(boolean active) { this.active = active; }

    /** Role discriminator stored in the database, e.g. "RECEPTIONIST". */
    public abstract String getRole();

    /** Title shown at the top of the dashboard for this role. */
    public abstract String getDashboardTitle();

    /** Whether this role is allowed to manage staff accounts. */
    public abstract boolean canManageStaff();

    /** Client-safe JSON representation — the password hash is deliberately left out. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", userId);
        m.put("username", username);
        m.put("fullName", fullName);
        m.put("role", getRole());
        m.put("active", active);
        return m;
    }
}
