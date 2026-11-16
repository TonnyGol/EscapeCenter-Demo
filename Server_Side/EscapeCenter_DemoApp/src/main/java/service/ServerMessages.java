package service;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns the English reasons the server gives into Hebrew sentences for the staff. The server keeps its English
 * text so the API stays the same for other callers; only what the staff reads is translated here.
 */
public final class ServerMessages {

    private static final Map<String, String> KNOWN_REASONS = Map.of(
            "This slot is already booked.", "המשבצת הזו כבר תפוסה. בחרו שעה אחרת.",
            "Booking not found.", "ההזמנה לא נמצאה. ייתכן שנמחקה, רעננו את הלוח.",
            "Client not found.", "הלקוח לא נמצא. ייתכן שנמחק, פתחו את מסך הלקוחות מחדש.",
            "Room not found.", "החדר לא נמצא.",
            "The booking could not be saved. Please try again.", "ההזמנה לא נשמרה. נסו שוב.",
            "Database error. Please try again later.", "שגיאה בשרת. נסו שוב מאוחר יותר.");

    private static final Map<String, String> FIELD_NAMES = Map.of(
            "client.firstName", "שם פרטי",
            "client.lastName", "שם משפחה",
            "client.phoneNumber", "טלפון",
            "client.email", "אימייל",
            "firstName", "שם פרטי",
            "lastName", "שם משפחה",
            "phoneNumber", "טלפון",
            "email", "אימייל",
            "booking.participants", "מספר המשתתפים");

    private static final Pattern FIELD_PROBLEM = Pattern.compile("^([\\w.]+): (.+)$");
    private static final Pattern MAX_PEOPLE = Pattern.compile("holds at most (\\d+) people");
    private static final Pattern SIZE_LIMIT = Pattern.compile("size must be between \\d+ and (\\d+)");
    private static final String PROBLEM_SEPARATOR = "; ";
    private static final String HTTP_PREFIX = "HTTP ";
    private static final String SERVER_ERROR_MESSAGE = "השרת החזיר שגיאה (%s). נסו שוב.";
    private static final String UNKNOWN_REASON_MESSAGE = "הפעולה נכשלה. פרטים טכניים: %s";

    private ServerMessages() {
    }

    /** A sentence for the staff. Reasons that are already Hebrew pass through; unknown English ones are kept as a technical detail. */
    public static String inHebrew(String reason) {
        if (reason == null || reason.isBlank()) {
            return String.format(UNKNOWN_REASON_MESSAGE, "");
        }
        if (reason.startsWith(HTTP_PREFIX)) {
            return String.format(SERVER_ERROR_MESSAGE, reason.substring(HTTP_PREFIX.length()));
        }
        return Arrays.stream(reason.split(PROBLEM_SEPARATOR))
                .map(ServerMessages::oneProblem)
                .collect(Collectors.joining(" "));
    }

    private static String oneProblem(String problem) {
        String known = KNOWN_REASONS.get(problem);
        if (known != null) {
            return known;
        }
        Matcher field = FIELD_PROBLEM.matcher(problem);
        if (field.matches()) {
            return fieldProblem(field.group(1), field.group(2));
        }
        return isHebrew(problem) ? problem : String.format(UNKNOWN_REASON_MESSAGE, problem);
    }

    private static String fieldProblem(String field, String message) {
        String fieldName = FIELD_NAMES.get(field);
        if (message.startsWith("must not be blank")) {
            return fieldName == null ? "יש למלא את כל השדות החובה." : "יש למלא " + fieldName + ".";
        }
        if (message.startsWith("must be a well-formed email")) {
            return "כתובת האימייל לא תקינה.";
        }
        if (message.contains("must contain digits")) {
            return "הטלפון צריך לכלול ספרות.";
        }
        if (message.contains("another client already has this phone")) {
            return "ללקוח אחר כבר יש את מספר הטלפון הזה.";
        }
        Matcher people = MAX_PEOPLE.matcher(message);
        if (people.find()) {
            return "החדר מכיל עד " + people.group(1) + " משתתפים.";
        }
        if (message.startsWith("must be greater than or equal to")) {
            return "מספר המשתתפים חייב להיות לפחות 1.";
        }
        Matcher size = SIZE_LIMIT.matcher(message);
        if (size.find()) {
            return (fieldName == null ? "הטקסט" : fieldName) + " ארוך מדי (עד " + size.group(1) + " תווים).";
        }
        if (field.equals("startAt") && message.contains("not a game start time")) {
            return "השעה הזו אינה שעת משחק של החדר.";
        }
        return String.format(UNKNOWN_REASON_MESSAGE, field + ": " + message);
    }

    private static boolean isHebrew(String text) {
        return text.chars().anyMatch(c -> c >= 0x0590 && c <= 0x05FF);
    }
}
