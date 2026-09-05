package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;


public class Appointment {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String appointmentNumber;
    private String patientId;
    private String dentistId;
    private String treatmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;

    // Convenience / display-only fields (populated by joined DAO queries)
    private String patientName;
    private String patientContact;
    private String patientAddress;
    private String dentistName;
    private String treatmentName;
    private Double treatmentCost;

    public Appointment() { }

    public Appointment(String appointmentNumber, String patientId, String dentistId,
                        String treatmentId, LocalDate appointmentDate,
                        LocalTime appointmentTime, String status) {
        this.appointmentNumber = appointmentNumber;
        this.patientId = patientId;
        this.dentistId = dentistId;
        this.treatmentId = treatmentId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    public String getAppointmentNumber() { return appointmentNumber; }
    public String getPatientId() { return patientId; }
    public String getDentistId() { return dentistId; }
    public String getTreatmentId() { return treatmentId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public String getStatus() { return status; }
    public String getPatientName() { return patientName; }
    public String getPatientContact() { return patientContact; }
    public String getPatientAddress() { return patientAddress; }
    public String getDentistName() { return dentistName; }
    public String getTreatmentName() { return treatmentName; }
    public Double getTreatmentCost() { return treatmentCost; }

    public void setAppointmentNumber(String v) { this.appointmentNumber = v; }
    public void setPatientId(String v) { this.patientId = v; }
    public void setDentistId(String v) { this.dentistId = v; }
    public void setTreatmentId(String v) { this.treatmentId = v; }
    public void setAppointmentDate(LocalDate v) { this.appointmentDate = v; }
    public void setAppointmentTime(LocalTime v) { this.appointmentTime = v; }
    public void setStatus(String v) { this.status = v; }
    public void setPatientName(String v) { this.patientName = v; }
    public void setPatientContact(String v) { this.patientContact = v; }
    public void setPatientAddress(String v) { this.patientAddress = v; }
    public void setDentistName(String v) { this.dentistName = v; }
    public void setTreatmentName(String v) { this.treatmentName = v; }
    public void setTreatmentCost(Double v) { this.treatmentCost = v; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appointmentNumber", appointmentNumber);
        m.put("patientId", patientId);
        m.put("dentistId", dentistId);
        m.put("treatmentId", treatmentId);
        m.put("appointmentDate", appointmentDate == null ? null : appointmentDate.toString());
        m.put("appointmentTime", appointmentTime == null ? null : appointmentTime.toString());
        m.put("status", status);
        m.put("patientName", patientName);
        m.put("patientContact", patientContact);
        m.put("patientAddress", patientAddress);
        m.put("dentistName", dentistName);
        m.put("treatmentName", treatmentName);
        m.put("treatmentCost", treatmentCost);
        return m;
    }
}
