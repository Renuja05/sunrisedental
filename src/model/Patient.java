package model;

import java.util.LinkedHashMap;
import java.util.Map;


public class Patient {

    private String patientId;
    private String name;
    private String address;
    private String contactNumber;

    public Patient() { }

    public Patient(String patientId, String name, String address, String contactNumber) {
        this.patientId = patientId;
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public String getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }

    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientId", patientId);
        m.put("name", name);
        m.put("address", address);
        m.put("contactNumber", contactNumber);
        return m;
    }

    @Override
    public String toString() {
        return name + " (" + contactNumber + ")";
    }
}
