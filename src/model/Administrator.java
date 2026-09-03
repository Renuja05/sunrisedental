package model;

/** Elevated account: everything a Receptionist can do, plus staff management. */
public class Administrator extends User {

    public Administrator(String userId, String username, String passwordHash,
                          String fullName, boolean active) {
        super(userId, username, passwordHash, fullName, active);
    }

    @Override
    public String getRole() { return "ADMINISTRATOR"; }

    @Override
    public String getDashboardTitle() { return "Administrator Dashboard"; }

    @Override
    public boolean canManageStaff() { return true; }
}
