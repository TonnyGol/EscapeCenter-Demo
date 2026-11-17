package model;

/**
 * Who the game is for. The constant names are what the server stores and sends.
 * To add a type, add a constant here and in the server's Entities.BookingType.
 */
public enum BookingType {
    CLIENT("לקוח פרטי"),
    COMPANY("חברה"),
    TEAM_BUILDING("גיבוש");

    private final String hebrewLabel;

    BookingType(String hebrewLabel) {
        this.hebrewLabel = hebrewLabel;
    }

    /** What the type choice box shows. JSON still uses the constant name. */
    @Override
    public String toString() {
        return hebrewLabel;
    }
}
