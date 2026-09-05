package model;

import java.util.LinkedHashMap;
import java.util.Map;


public class Dentist {

    private String dentistId;
    private String name;
    private String specialization;
    private boolean active;

    public Dentist() { }

    public Dentist(String dentistId, String name, String specialization, boolean active) {
        this.dentistId = dentistId;
        this.name = name;
        this.specialization = specialization;
        this.active = active;
    }

    public String getDentistId() { return dentistId; }
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public boolean isActive() { return active; }

    public void setDentistId(String dentistId) { this.dentistId = dentistId; }
    public void setName(String name) { this.name = name; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public void setActive(boolean active) { this.active = active; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dentistId", dentistId);
        m.put("name", name);
        m.put("specialization", specialization);
        m.put("active", active);
        return m;
    }

    @Override
    public String toString() {
        return name + " (" + specialization + ")";
    }
}
