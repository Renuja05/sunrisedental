package util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;


public final class Validator {

    private Validator() { }

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z.'\\- ]{1,79}$");

    // Accepts local Sri Lankan format 0XXXXXXXXX or international +94XXXXXXXXX
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(0\\d{9}|\\+94\\d{9})$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]{2,19}$");

    public static final LocalTime CLINIC_OPEN = LocalTime.of(8, 0);
    public static final LocalTime CLINIC_CLOSE = LocalTime.of(17, 0);

    public static String requiredText(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            return fieldLabel + " is required.";
        }
        return null;
    }

    public static String patientName(String value) {
        String required = requiredText(value, "Patient name");
        if (required != null) return required;
        if (!NAME_PATTERN.matcher(value.trim()).matches()) {
            return "Patient name may only contain letters, spaces, apostrophes and hyphens (2-80 characters).";
        }
        return null;
    }

    public static String address(String value) {
        String required = requiredText(value, "Address");
        if (required != null) return required;
        if (value.trim().length() < 5 || value.trim().length() > 200) {
            return "Address must be between 5 and 200 characters.";
        }
        return null;
    }

    public static String contactNumber(String value) {
        String required = requiredText(value, "Contact number");
        if (required != null) return required;
        if (!PHONE_PATTERN.matcher(value.trim()).matches()) {
            return "Contact number must be in the form 07XXXXXXXX or +947XXXXXXXX.";
        }
        return null;
    }

    public static String username(String value) {
        String required = requiredText(value, "Username");
        if (required != null) return required;
        if (!USERNAME_PATTERN.matcher(value.trim()).matches()) {
            return "Username must start with a letter and be 3-20 letters, digits or underscores.";
        }
        return null;
    }

    public static String password(String value) {
        if (value == null || value.isEmpty()) {
            return "Password is required.";
        }
        if (value.length() < 6) {
            return "Password must be at least 6 characters long.";
        }
        return null;
    }

    public static String fullName(String value) {
        return patientName(value);
    }

    public static String selection(Object value, String fieldLabel) {
        if (value == null) return fieldLabel + " must be selected.";
        return null;
    }

    public static String appointmentDate(LocalDate date) {
        if (date == null) return "Appointment date is required.";
        if (date.isBefore(LocalDate.now())) {
            return "Appointment date cannot be in the past.";
        }
        if (date.isAfter(LocalDate.now().plusMonths(6))) {
            return "Appointment date cannot be more than 6 months in the future.";
        }
        return null;
    }

    public static String appointmentTime(LocalTime time) {
        if (time == null) return "Appointment time is required.";
        if (time.isBefore(CLINIC_OPEN) || time.isAfter(CLINIC_CLOSE.minusMinutes(1))) {
            return "Appointment time must be between " + CLINIC_OPEN + " and " + CLINIC_CLOSE + ".";
        }
        if (time.getMinute() % 15 != 0) {
            return "Appointment time must fall on a 15-minute slot (e.g. 09:00, 09:15, 09:30).";
        }
        return null;
    }

    public static String appointmentNumber(String value) {
        return requiredText(value, "Appointment number");
    }
}
