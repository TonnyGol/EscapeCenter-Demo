package model;

/**
 * Where a booking stands. The constant names are what the server stores and sends.
 * CANCELLED is never set from the staff app: cancelling deletes the booking and frees the slot.
 */
public enum BookingStatus {
    CONFIRMED("מאושרת"),
    COMPLETED("הגיעו"),
    NO_SHOW("לא הגיעו"),
    CANCELLED("בוטלה");

    private final String hebrewLabel;

    BookingStatus(String hebrewLabel) {
        this.hebrewLabel = hebrewLabel;
    }

    /** What the choice boxes show. JSON still uses the constant name. */
    @Override
    public String toString() {
        return hebrewLabel;
    }
}
