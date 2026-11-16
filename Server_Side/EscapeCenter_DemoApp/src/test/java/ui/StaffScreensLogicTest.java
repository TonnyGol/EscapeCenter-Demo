package ui;

import model.Booking;
import model.BookingStatus;
import model.BookingType;
import model.Client;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The parts of the staff screens that decide something, tested without opening a window. */
class StaffScreensLogicTest {

    private static final Client DANA = new Client(1L, "Dana", "Levi", "0501234567", null);

    private static Booking game(String room, String startAt, String endAt) {
        return new Booking(1L, room, startAt, endAt, DANA, 2, "beginner", null, null, BookingType.CLIENT, null, BookingStatus.CONFIRMED, null);
    }

    @Test
    void statusLine_marksArrivedAndNoShow_andStaysEmptyForConfirmed() {
        assertEquals("", BookingManagementScreen.statusLine(BookingStatus.CONFIRMED));
        assertEquals("\n✓ הגיעו", BookingManagementScreen.statusLine(BookingStatus.COMPLETED));
        assertEquals("\n✕ לא הגיעו", BookingManagementScreen.statusLine(BookingStatus.NO_SHOW));
        assertEquals("\nבוטלה", BookingManagementScreen.statusLine(BookingStatus.CANCELLED));
    }

    @Test
    void overlaps_whenTwoGamesShareMinutes_evenIfTheyStartAtDifferentTimes() {
        Booking early = game("Room 1", "2026-11-17T18:30", "2026-11-17T19:15");
        Booking late = game("Room 2", "2026-11-17T19:00", "2026-11-17T20:00");

        assertTrue(early.overlaps(late));
        assertTrue(late.overlaps(early));
    }

    @Test
    void doesNotOverlap_whenOneGameStartsExactlyAsTheOtherEnds() {
        Booking first = game("Room 1", "2026-11-17T18:00", "2026-11-17T19:00");
        Booking second = game("Room 2", "2026-11-17T19:00", "2026-11-17T20:00");

        assertFalse(first.overlaps(second));
    }

    @Test
    void countGames_countsOnlyThePeriod_andPlayedOnlyAfterTheEndTime() {
        List<Booking> bookings = List.of(
                game("Room 1", "2026-11-15T10:00", "2026-11-15T11:00"),   // ended
                game("Room 1", "2026-11-17T18:00", "2026-11-17T19:00"),   // running now
                game("Room 1", "2026-11-21T20:00", "2026-11-21T21:00"),   // later this week
                game("Room 1", "2026-11-22T10:00", "2026-11-22T11:00"));  // next week
        LocalDateTime now = LocalDateTime.parse("2026-11-17T18:30");

        ReportsScreen.GamesCount week = ReportsScreen.countGames(bookings,
                LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-21"), now);

        assertEquals(3, week.booked());
        assertEquals(1, week.played());
        assertEquals(2, week.remaining());
    }

    @Test
    void overlapSpans_namesTheSharedMinutes_andJoinsThreeWayOverlapsIntoOneSpan() {
        List<Booking> bookings = List.of(
                game("Room 1", "2026-11-17T18:30", "2026-11-17T19:15"),
                game("Room 2", "2026-11-17T19:00", "2026-11-17T20:00"),
                game("Room 3", "2026-11-17T19:10", "2026-11-17T20:30"),
                game("Room 1", "2026-11-17T22:00", "2026-11-17T23:00"));

        List<DayTimelineView.Span> spans = DayTimelineView.overlapSpans(bookings);

        assertEquals(1, spans.size());
        assertEquals(LocalDateTime.parse("2026-11-17T19:00"), spans.get(0).start());
        assertEquals(LocalDateTime.parse("2026-11-17T20:00"), spans.get(0).end());
    }

    @Test
    void overlapSpans_isEmpty_whenGamesOnlyTouch_orShareARoom() {
        List<Booking> bookings = List.of(
                game("Room 1", "2026-11-17T18:00", "2026-11-17T19:00"),
                game("Room 2", "2026-11-17T19:00", "2026-11-17T20:00"));

        assertTrue(DayTimelineView.overlapSpans(bookings).isEmpty());
    }

    @Test
    void overlapEntries_listEachPairEarliestFirst_andOnlyRoomKeepsItsOwnPairsFirst() {
        List<Booking> bookings = List.of(
                game("Room 1", "2026-11-17T18:30", "2026-11-17T19:15"),
                game("Room 2", "2026-11-17T19:00", "2026-11-17T20:00"),
                game("Room 3", "2026-11-17T10:00", "2026-11-17T11:30"),
                game("Room 1", "2026-11-18T10:45", "2026-11-18T12:15"),
                game("Room 3", "2026-11-18T10:00", "2026-11-18T11:30"));

        List<OverlapPanel.Entry> all = OverlapPanel.entries(bookings, null);
        List<OverlapPanel.Entry> forRoomTwo = OverlapPanel.entries(bookings, "Room 2");

        assertEquals(2, all.size());
        assertEquals(LocalDate.parse("2026-11-17"), all.get(0).date());
        assertEquals(LocalDateTime.parse("2026-11-17T19:00"), all.get(0).start());
        assertEquals(LocalDateTime.parse("2026-11-17T19:15"), all.get(0).end());
        assertEquals(LocalDateTime.parse("2026-11-18T10:45"), all.get(1).start());
        assertEquals(1, forRoomTwo.size());
        assertEquals("Room 2", forRoomTwo.get(0).firstRoom());
        assertEquals("Room 1", forRoomTwo.get(0).secondRoom());
    }

    @Test
    void firstProblem_findsTheFirstWrongFieldInFormOrder() {
        assertEquals(FormChecks.Field.FIRST_NAME,
                FormChecks.firstProblem(" ", "", "", "").orElseThrow().field());
        assertEquals(FormChecks.Field.PHONE,
                FormChecks.firstProblem("Dana", "Levi", "---", "").orElseThrow().field());
        assertEquals(FormChecks.Field.EMAIL,
                FormChecks.firstProblem("Dana", "Levi", "050-123", "dana@").orElseThrow().field());
        assertTrue(FormChecks.firstProblem("Dana", "Levi", "050-123", "").isEmpty());
        assertTrue(FormChecks.firstProblem("Dana", "Levi", "050-123", "dana@example.com").isEmpty());
    }

    @Test
    void whatsAppLink_turnsALocalIsraeliNumberIntoInternationalForm() {
        assertEquals("https://wa.me/972501234567", ClientsManagementScreen.whatsAppLink("050-123 4567"));
        assertEquals("https://wa.me/972501234567", ClientsManagementScreen.whatsAppLink("+972501234567"));
    }
}
