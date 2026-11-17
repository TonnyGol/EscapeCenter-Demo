package ui;

import model.Booking;
import service.ApiService;
import service.Session;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Dashboard screen: bookings today and this week and the next game, above the quick-action card grid.
 * Returns a pane for embedding inside NavigationShell.
 */
public class DashboardScreen {

    private static final double CARD_ICON_SIZE = 36;
    private static final String CARD_ICON_COLOR = "#c8452d";
    private static final String TODAY_HEADING = "היום";
    private static final String SHORTCUTS_HEADING = "קיצורי דרך";
    private static final String NO_NUMBER_YET = "–";
    private static final String LOAD_FAILED_MESSAGE = "לא ניתן לטעון את הנתונים. בדקו את החיבור לשרת.";
    private static final String BOOKED_TODAY_CAPTION = "הוזמנו היום";
    private static final String BOOKED_THIS_WEEK_CAPTION = "הוזמנו השבוע";
    private static final String NEXT_GAME_CAPTION = "המשחק הבא היום";
    private static final String NO_MORE_GAMES_CAPTION = "אין עוד משחקים היום";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public Node createPane(NavigationShell shell) {
        String username = Session.getInstance().getUsername();

        // Welcome header
        Label welcome = new Label("שלום, " + (username != null ? username : ""));
        welcome.getStyleClass().add("page-title");

        Label sub = new Label("מה תרצה לעשות היום?");
        sub.getStyleClass().add("dashboard-sub");

        // Quick-action cards
        VBox bookingCard = createCard(Icons.CALENDAR, "ניהול הזמנות",
                "צפייה ועריכת הזמנות בלוח השבועי");
        VBox clientCard = createCard(Icons.USERS, "ניהול לקוחות",
                "חיפוש וצפייה בפרטי לקוחות");
        VBox reportsCard = createCard(Icons.REPORT, "דו\"חות",
                "משחקים ששוחקו והוזמנו השבוע והחודש");

        // Card actions
        bookingCard.setOnMouseClicked(e -> shell.navigateToBookings());
        clientCard.setOnMouseClicked(e -> shell.navigateToClients());
        reportsCard.setOnMouseClicked(e -> shell.navigateToReports());

        // One row of three in the order of the sidebar; in a narrow window the last card wraps to a second row.
        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("dashboard-grid");
        // In a right-to-left pane the left alignments sit on the reading start, next to the sidebar.
        grid.setAlignment(Pos.TOP_LEFT);
        grid.getChildren().addAll(bookingCard, clientCard, reportsCard);

        VBox pane = new VBox(20, welcome, sub, sectionTitle(TODAY_HEADING), createTodayStats(),
                sectionTitle(SHORTCUTS_HEADING), grid);
        pane.getStyleClass().add("dashboard-pane");
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        pane.setAlignment(Pos.TOP_LEFT);

        // In a small window the cards wrap into more rows than fit, so the dashboard scrolls like the other screens.
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        return scrollPane;
    }

    /** Today's and this week's bookings and the next game, filled in from the server in the background so the dashboard opens at once. */
    private VBox createTodayStats() {
        Label bookedToday = statNumber();
        Label bookedThisWeek = statNumber();
        Label nextGameTime = statNumber();
        Label nextGameCaption = statCaption(NEXT_GAME_CAPTION);
        FlowPane tiles = new FlowPane();
        tiles.getStyleClass().add("dashboard-grid");
        tiles.setAlignment(Pos.TOP_LEFT);
        tiles.getChildren().addAll(statTile(bookedToday, statCaption(BOOKED_TODAY_CAPTION)),
                statTile(bookedThisWeek, statCaption(BOOKED_THIS_WEEK_CAPTION)),
                statTile(nextGameTime, nextGameCaption));

        Label failureNote = new Label();
        failureNote.getStyleClass().add("dashboard-sub");
        loadStats(bookedToday, bookedThisWeek, nextGameTime, nextGameCaption, failureNote);
        return new VBox(8, tiles, failureNote);
    }

    private static void loadStats(Label bookedToday, Label bookedThisWeek, Label nextGameTime, Label nextGameCaption, Label failureNote) {
        Thread loader = new Thread(() -> {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = ReportsScreen.weekStartOf(today);
            LocalDate weekEnd = ReportsScreen.weekEndOf(today);
            // One request covers both: today is inside the week.
            Optional<Map<String, Booking>> loaded = ApiService.getInstance().loadBookings(weekStart, weekEnd);
            Platform.runLater(() -> {
                if (loaded.isEmpty()) {
                    failureNote.setText(LOAD_FAILED_MESSAGE);
                    return;
                }
                Collection<Booking> bookings = loaded.get().values();
                LocalDateTime now = LocalDateTime.now();
                bookedToday.setText(String.valueOf(ReportsScreen.countGames(bookings, today, today, now).booked()));
                bookedThisWeek.setText(String.valueOf(ReportsScreen.countGames(bookings, weekStart, weekEnd, now).booked()));
                nextGameToday(bookings, now).ifPresentOrElse(game -> {
                    nextGameTime.setText(game.start().format(TIME_FORMAT));
                    nextGameCaption.setText(NEXT_GAME_CAPTION + " · " + game.roomName());
                }, () -> nextGameCaption.setText(NO_MORE_GAMES_CAPTION));
            });
        });
        loader.setDaemon(true); // a slow server must not keep the app open after the window closes
        loader.start();
    }

    /** The game that starts soonest after {@code now}, on the same day. */
    static Optional<Booking> nextGameToday(Collection<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(game -> game.start().isAfter(now) && game.start().toLocalDate().equals(now.toLocalDate()))
                .min(Comparator.comparing(Booking::start));
    }

    private static Label statNumber() {
        Label number = new Label(NO_NUMBER_YET);
        number.getStyleClass().add("dashboard-stat-number");
        return number;
    }

    private static Label statCaption(String text) {
        Label caption = new Label(text);
        caption.getStyleClass().add("dashboard-stat-caption");
        return caption;
    }

    private static VBox statTile(Label number, Label caption) {
        VBox tile = new VBox(2, number, caption);
        tile.getStyleClass().add("dashboard-stat");
        tile.setAlignment(Pos.CENTER);
        // A screen reader reads the tile as one fact, "caption: number", not as two loose labels.
        tile.accessibleTextProperty().bind(Bindings.concat(caption.textProperty(), ": ", number.textProperty()));
        return tile;
    }

    private static Label sectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("dashboard-section-title");
        return title;
    }

    private VBox createCard(String iconPath, String title, String description) {
        Node iconLabel = Icons.of(iconPath, CARD_ICON_SIZE, Color.web(CARD_ICON_COLOR));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-card-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("dashboard-card-desc");
        descLabel.setWrapText(true);

        VBox card = new VBox(8, iconLabel, titleLabel, descLabel);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.CENTER);
        card.setAccessibleText(title + " — " + description);

        Tooltip tooltip = new Tooltip(description);
        Tooltip.install(card, tooltip);

        return card;
    }
}
