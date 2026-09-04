package model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** The receipt generated for one appointment. */
public class Bill {

    private String billNumber;
    private String appointmentNumber;
    private double consultationFee;
    private double treatmentCost;
    private double discount;
    private double totalAmount;
    private LocalDateTime billDate;

    // Display-only convenience fields
    private String patientName;
    private String dentistName;
    private String treatmentName;

    public Bill() { }

    public Bill(String billNumber, String appointmentNumber, double consultationFee,
                double treatmentCost, double discount, double totalAmount) {
        this.billNumber = billNumber;
        this.appointmentNumber = appointmentNumber;
        this.consultationFee = consultationFee;
        this.treatmentCost = treatmentCost;
        this.discount = discount;
        this.totalAmount = totalAmount;
    }

    public String getBillNumber() { return billNumber; }
    public String getAppointmentNumber() { return appointmentNumber; }
    public double getConsultationFee() { return consultationFee; }
    public double getTreatmentCost() { return treatmentCost; }
    public double getDiscount() { return discount; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDateTime getBillDate() { return billDate; }
    public String getPatientName() { return patientName; }
    public String getDentistName() { return dentistName; }
    public String getTreatmentName() { return treatmentName; }

    public void setBillNumber(String v) { this.billNumber = v; }
    public void setAppointmentNumber(String v) { this.appointmentNumber = v; }
    public void setConsultationFee(double v) { this.consultationFee = v; }
    public void setTreatmentCost(double v) { this.treatmentCost = v; }
    public void setDiscount(double v) { this.discount = v; }
    public void setTotalAmount(double v) { this.totalAmount = v; }
    public void setBillDate(LocalDateTime v) { this.billDate = v; }
    public void setPatientName(String v) { this.patientName = v; }
    public void setDentistName(String v) { this.dentistName = v; }
    public void setTreatmentName(String v) { this.treatmentName = v; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("billNumber", billNumber);
        m.put("appointmentNumber", appointmentNumber);
        m.put("consultationFee", consultationFee);
        m.put("treatmentCost", treatmentCost);
        m.put("discount", discount);
        m.put("totalAmount", totalAmount);
        m.put("billDate", billDate == null ? null : billDate.toString());
        m.put("patientName", patientName);
        m.put("dentistName", dentistName);
        m.put("treatmentName", treatmentName);
        return m;
    }
}
