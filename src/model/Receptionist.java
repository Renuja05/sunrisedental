package model;

/** Day-to-day clinic staff account: appointments, search, billing. */
public class Receptionist extends User {

    public Receptionist(String userId, String username, String passwordHash,
                         String fullName, boolean active) {
        super(userId, username, passwordHash, fullName, active);
    }

    @Override
    public String getRole() { return "RECEPTIONIST"; }

    @Override
    public String getDashboardTitle() { return "Receptionist Dashboard"; }

    @Override
    public boolean canManageStaff() { return false; }
}
