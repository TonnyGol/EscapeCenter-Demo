package ui;

import model.Booking;
import service.ApiService;
import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Reports screen: how many games were played so far and how many are booked, for the current week and month.
 * Returns a pane for embedding in NavigationShell.
 */
public class ReportsScreen {

    private static final String LOADING_MESSAGE = "טוענים את הנתונים…";
    private static final String LOAD_FAILED_MESSAGE = "לא ניתן לטעון את הנתונים. בדקו את החיבור לשרת ולחצו על רענון.";
    private static final DateTimeFormatter RANGE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter UPDATED_AT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DAYS_IN_WEEK = 7;
    private static final int PERIOD_COLUMN = 0;
    private static final int PLAYED_COLUMN = 1;
    private static final int BOOKED_COLUMN = 2;
    private static final int REMAINING_COLUMN = 3;
    private static final int COLUMN_COUNT = 4;

    /** Games of one period: those that already ended, and every game booked in it. */
    record GamesCount(long played, long booked) {
        long remaining() {
            return booked - played;
        }
    }

    private final GridPane grid = new GridPane();
    private final Label statusLabel = new Label();

    public Node createPane() {
        Label title = new Label("דו\"חות משחקים");
        title.getStyleClass().add("page-title");

        Button refreshBtn = new Button("רענון");
        refreshBtn.getStyleClass().add("week-nav-btn");
        refreshBtn.setTooltip(new Tooltip("טעינת הנתונים מחדש מהשרת"));
        refreshBtn.setOnAction(e -> reload());

        grid.getStyleClass().add("reports-grid");
        grid.setMaxWidth(Region.USE_PREF_SIZE); // the card fits its columns instead of stretching across the window
        statusLabel.getStyleClass().add("reports-note");
        reload();

        VBox pane = new VBox(16, title, grid, statusLabel, refreshBtn);
        pane.getStyleClass().add("reports-pane");
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        return pane;
    }

    private void reload() {
        LocalDate today = LocalDate.now();
        // The week runs Sunday to Saturday, like the booking calendar.
        LocalDate weekStart = weekStartOf(today);
        LocalDate weekEnd = weekEndOf(today);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        // One request covers both periods: a week can start in the previous month or end in the next one.
        LocalDate from = weekStart.isBefore(monthStart) ? weekStart : monthStart;
        LocalDate to = weekEnd.isAfter(monthEnd) ? weekEnd : monthEnd;
        statusLabel.setText(LOADING_MESSAGE);
        // The request runs off the screen thread, so a slow or stopped server does not freeze the window.
        Thread loader = new Thread(() -> {
            Optional<Map<String, Booking>> loaded = ApiService.getInstance().loadBookings(from, to);
            Platform.runLater(() -> showCounts(loaded, weekStart, weekEnd, monthStart, monthEnd));
        });
        loader.setDaemon(true);
        loader.start();
    }

    private void showCounts(Optional<Map<String, Booking>> loaded, LocalDate weekStart, LocalDate weekEnd,
                            LocalDate monthStart, LocalDate monthEnd) {
        if (loaded.isEmpty()) {
            statusLabel.setText(LOAD_FAILED_MESSAGE);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Collection<Booking> bookings = loaded.get().values();
        grid.getChildren().clear();
        addHeaderRow();
        addPeriodRow(1, "השבוע", weekStart, weekEnd, countGames(bookings, weekStart, weekEnd, now));
        addPeriodRow(4, "החודש", monthStart, monthEnd, countGames(bookings, monthStart, monthEnd, now));
        statusLabel.setText("עודכן בשעה " + now.format(UPDATED_AT_FORMAT) + ". משחק נספר כ\"שוחק\" אחרי שעת הסיום שלו.");
    }

    /** The week runs Sunday to Saturday, like the booking calendar. */
    static LocalDate weekStartOf(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    static LocalDate weekEndOf(LocalDate day) {
        return weekStartOf(day).plusDays(DAYS_IN_WEEK - 1);
    }

    /** Counts the games that start between the two dates, both included. */
    static GamesCount countGames(Collection<Booking> bookings, LocalDate from, LocalDate to, LocalDateTime now) {
        long booked = 0;
        long played = 0;
        for (Booking booking : bookings) {
            LocalDate gameDay = booking.start().toLocalDate();
            if (gameDay.isBefore(from) || gameDay.isAfter(to)) {
                continue;
            }
            booked++;
            if (!booking.end().isAfter(now)) {
                played++;
            }
        }
        return new GamesCount(played, booked);
    }

    private void addHeaderRow() {
        grid.add(headerLabel("תקופה"), PERIOD_COLUMN, 0);
        grid.add(headerLabel("שוחקו עד כה"), PLAYED_COLUMN, 0);
        grid.add(headerLabel("הוזמנו בסך הכול"), BOOKED_COLUMN, 0);
        grid.add(headerLabel("עוד לא שוחקו"), REMAINING_COLUMN, 0);
    }

    private void addPeriodRow(int row, String name, LocalDate from, LocalDate to, GamesCount count) {
        Region divider = new Region();
        divider.getStyleClass().add("reports-row-divider");
        grid.add(divider, 0, row, COLUMN_COUNT, 1);

        Label period = new Label(name);
        period.getStyleClass().add("reports-period");
        Label range = new Label(from.format(RANGE_DATE_FORMAT) + " - " + to.format(RANGE_DATE_FORMAT));
        range.getStyleClass().add("reports-range");
        grid.add(period, PERIOD_COLUMN, row + 1);
        grid.add(range, PERIOD_COLUMN, row + 2);

        Label played = numberLabel(count.played());
        played.getStyleClass().add("reports-number-played");
        grid.add(played, PLAYED_COLUMN, row + 1, 1, 2);
        grid.add(numberLabel(count.booked()), BOOKED_COLUMN, row + 1, 1, 2);
        grid.add(numberLabel(count.remaining()), REMAINING_COLUMN, row + 1, 1, 2);
    }

    private static Label headerLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("reports-head");
        return label;
    }

    private static Label numberLabel(long number) {
        Label label = new Label(String.valueOf(number));
        label.getStyleClass().add("reports-number");
        return label;
    }
}
