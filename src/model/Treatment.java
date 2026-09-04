package model;

import java.util.LinkedHashMap;
import java.util.Map;

/** A treatment type and its standard cost. */
public class Treatment {

    private String treatmentId;
    private String treatmentName;
    private double cost;

    public Treatment() { }

    public Treatment(String treatmentId, String treatmentName, double cost) {
        this.treatmentId = treatmentId;
        this.treatmentName = treatmentName;
        this.cost = cost;
    }

    public String getTreatmentId() { return treatmentId; }
    public String getTreatmentName() { return treatmentName; }
    public double getCost() { return cost; }

    public void setTreatmentId(String treatmentId) { this.treatmentId = treatmentId; }
    public void setTreatmentName(String treatmentName) { this.treatmentName = treatmentName; }
    public void setCost(double cost) { this.cost = cost; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("treatmentId", treatmentId);
        m.put("treatmentName", treatmentName);
        m.put("cost", cost);
        return m;
    }

    @Override
    public String toString() {
        return treatmentName + " (Rs. " + String.format("%.2f", cost) + ")";
    }
}
