package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMessagesTest {

    @Test
    void knownReasons_becomeHebrewSentences() {
        assertEquals("המשבצת הזו כבר תפוסה. בחרו שעה אחרת.", ServerMessages.inHebrew("This slot is already booked."));
        assertEquals("ההזמנה לא נמצאה. ייתכן שנמחקה, רעננו את הלוח.", ServerMessages.inHebrew("Booking not found."));
    }

    @Test
    void fieldProblems_nameTheFieldInHebrew_andJoinSeveralProblems() {
        String message = ServerMessages.inHebrew("client.firstName: must not be blank; client.email: must be a well-formed email address");

        assertEquals("יש למלא שם פרטי. כתובת האימייל לא תקינה.", message);
    }

    @Test
    void capacityAndPhoneProblems_keepTheirNumbersAndMeaning() {
        assertEquals("החדר מכיל עד 8 משתתפים.", ServerMessages.inHebrew("booking.participants: this room holds at most 8 people."));
        assertEquals("ללקוח אחר כבר יש את מספר הטלפון הזה.",
                ServerMessages.inHebrew("client.phoneNumber: another client already has this phone number."));
    }

    @Test
    void statusCodesAndHebrewReasonsAreHandled_andUnknownEnglishIsKeptAsDetail() {
        assertEquals("השרת החזיר שגיאה (502). נסו שוב.", ServerMessages.inHebrew("HTTP 502"));
        assertEquals("לא ניתן להתחבר לשרת.", ServerMessages.inHebrew("לא ניתן להתחבר לשרת."));
        String unknown = ServerMessages.inHebrew("Something odd happened");
        assertTrue(unknown.contains("Something odd happened"));
        assertFalse(unknown.isBlank());
    }
}
