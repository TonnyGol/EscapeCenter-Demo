package ui;

import model.Booking;
import model.BookingDetails;
import model.BookingStatus;
import model.BookingType;
import model.Client;
import model.CreateBookingRequest;
import model.Room;
import model.UpdateBookingRequest;
import service.ApiService;
import service.ServerMessages;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Booking management screen — weekly calendar grid with room filter tabs,
 * week navigation, real-time polling, and dual layout modes.
 *
 * <ul>
 *   <li><b>Single room</b>: columns = days, rows = time slots (classic view)</li>
 *   <li><b>All rooms ("הכל")</b>: one day at a time, columns = rooms and time running down the page,
 *       so parallel games line up and the minutes they share are shaded (see {@link DayTimelineView})</li>
 * </ul>
 */
public class BookingManagementScreen {

    private final Map<String, Booking> bookings = new HashMap<>();
    private final Map<String, Button> slotButtons = new HashMap<>();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<DayOfWeek, String> HEBREW_DAYS = Map.of(
            DayOfWeek.SUNDAY, "ראשון",
            DayOfWeek.MONDAY, "שני",
            DayOfWeek.TUESDAY, "שלישי",
            DayOfWeek.WEDNESDAY, "רביעי",
            DayOfWeek.THURSDAY, "חמישי",
            DayOfWeek.FRIDAY, "שישי",
            DayOfWeek.SATURDAY, "שבת"
    );

    private static final int DAYS_IN_WEEK = 7;
    private static final String ALL_ROOMS_LABEL = "הכל";
    private static final String SAVE_FAILED_TITLE = "השמירה נכשלה";
    private static final String OVERLAP_PREFIX = "חפיפה ";
    private static final String SYNC_PENDING_LABEL = "מסנכרן…";
    private static final String SYNC_OK_LABEL = "מסונכרן ";
    private static final String SYNC_FAILED_LABEL = "אין חיבור לשרת";
    private static final Color SYNC_PENDING_COLOR = Color.web("#6f6b62");
    private static final Color SYNC_OK_COLOR = Color.web("#1a7f5a");
    private static final Color SYNC_FAILED_COLOR = Color.web("#b42318");
    private static final double SYNC_DOT_RADIUS = 5;
    private static final String CANCEL_CONFIRM_MESSAGE = "לבטל את ההזמנה של %s %s בחדר %s, %s? אי אפשר לשחזר אותה.";
    private static final String CANCEL_CONFIRM_TITLE = "ביטול הזמנה";
    private static final String CANCEL_CONFIRM_YES = "כן, בטל את ההזמנה";
    private static final String CANCEL_CONFIRM_NO = "השאר את ההזמנה";
    private static final DateTimeFormatter CANCEL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final List<String> SWATCH_COLORS = List.of("#fecaca", "#d1fae5", "#fef3c7", "#c7d2fe", "#e0e7ff");
    private static final List<String> SWATCH_NAMES = List.of("אדום", "ירוק", "צהוב", "כחול", "תכלת");
    private static final int MIN_STAFF_PARTICIPANTS = 2; // staff can book a pair even where customers need more
    private static final String SEARCH_BUTTON_LABEL = "חיפוש";
    private static final String TODAY_SUFFIX = " · היום";
    private static final String NO_GAMES_LABEL = "אין משחקים";
    private static final String ONE_GAME_LABEL = "משחק אחד";
    private static final String GAMES_LABEL = " משחקים";
    private static final DateTimeFormatter DAY_CHIP_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MODAL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Polling interval in seconds for real-time sync. */
    private static final int POLL_INTERVAL_SECONDS = 10;

    private List<Room> rooms = List.of();
    private Map<String, List<LocalTime>> gameStarts = Map.of(); // room name -> start time of each game
    private HBox roomFilter;
    private ToggleGroup roomGroup;
    private LocalDate weekStart;
    private LocalDate selectedDay; // the day the all-rooms view shows
    private HBox dayStrip;
    private ToggleButton allRoomsTab;
    private final OverlapPanel overlapPanel = new OverlapPanel();
    private Label weekLabel;
    private Pane calendarContainer; // HBox for single-room, ScrollPane-wrapped GridPane for all-rooms
    private ScrollPane scrollPane;
    private Stage ownerStage;
    private String selectedRoom = null; // null = show all rooms
    private Timeline pollingTimeline;

    /**
     * Creates the booking pane for embedding inside NavigationShell.
     */
    public Node createPane(Stage stage) {
        this.ownerStage = stage;

        fetchSchedule().ifPresent(this::applySchedule);

        // Start on current week (Sunday of this week)
        weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        selectedDay = LocalDate.now();
        reloadBookings();

        // ---- Toolbar: Week navigation ----
        Button prevBtn = new Button("◀ שבוע קודם");
        prevBtn.getStyleClass().add("week-nav-btn");
        prevBtn.setTooltip(new Tooltip("חזור שבוע אחורה"));
        prevBtn.setAccessibleText("שבוע קודם");

        Button nextBtn = new Button("שבוע הבא ▶");
        nextBtn.getStyleClass().add("week-nav-btn");
        nextBtn.setTooltip(new Tooltip("עבור לשבוע הבא"));
        nextBtn.setAccessibleText("שבוע הבא");

        Button todayBtn = new Button("היום");
        todayBtn.getStyleClass().add("week-nav-btn");
        todayBtn.setTooltip(new Tooltip("חזור לשבוע הנוכחי"));
        todayBtn.setAccessibleText("חזור לשבוע הנוכחי");

        weekLabel = new Label();
        weekLabel.getStyleClass().add("week-label");

        Label syncIndicator = new Label();
        showSync(syncIndicator, SYNC_PENDING_LABEL, SYNC_PENDING_COLOR);
        syncIndicator.getStyleClass().add("sync-indicator");
        syncIndicator.setTooltip(new Tooltip("סנכרון אוטומטי כל " + POLL_INTERVAL_SECONDS + " שניות"));

        prevBtn.setOnAction(e -> goToWeek(weekStart.minusWeeks(1)));
        nextBtn.setOnAction(e -> goToWeek(weekStart.plusWeeks(1)));
        todayBtn.setOnAction(e -> goToWeek(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))));

        Button searchBtn = new Button(SEARCH_BUTTON_LABEL);
        searchBtn.getStyleClass().add("week-nav-btn");
        searchBtn.setOnAction(e -> BookingSearchDialog.pick(ownerStage).ifPresent(this::showBooking));

        HBox toolbar = new HBox(10, nextBtn, weekLabel, prevBtn, todayBtn, searchBtn, syncIndicator);
        toolbar.getStyleClass().add("booking-toolbar");
        toolbar.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // ---- Room Filter Tabs ----
        roomFilter = new HBox(8);
        roomFilter.getStyleClass().add("room-filter-bar");
        roomFilter.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        allRoomsTab = createRoomTab(ALL_ROOMS_LABEL);
        allRoomsTab.setSelected(true);
        allRoomsTab.getStyleClass().add("room-tab-active");

        roomGroup = new ToggleGroup();
        allRoomsTab.setToggleGroup(roomGroup);
        roomFilter.getChildren().add(allRoomsTab);

        addRoomTabs();

        roomGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                allRoomsTab.setSelected(true);
                return;
            }
            for (Node node : roomFilter.getChildren()) {
                if (node instanceof ToggleButton tb) {
                    tb.getStyleClass().remove("room-tab-active");
                }
            }
            ((ToggleButton) newVal).getStyleClass().add("room-tab-active");

            String text = ((ToggleButton) newVal).getText();
            selectedRoom = text.equals(ALL_ROOMS_LABEL) ? null : text;
            rebuildCalendar();
        });

        // ---- Day strip (all-rooms view) ----
        dayStrip = new HBox(8);
        dayStrip.getStyleClass().add("day-strip");
        dayStrip.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // ---- Calendar Container ----
        calendarContainer = new HBox(8);
        calendarContainer.getStyleClass().add("calendar-container");
        ((HBox) calendarContainer).setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        scrollPane = new ScrollPane(calendarContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        rebuildCalendar();

        // In a right-to-left row the second child sits on the left, in the free space beside the calendar.
        HBox calendarRow = new HBox(12, scrollPane, overlapPanel.node());
        calendarRow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        VBox.setVgrow(calendarRow, Priority.ALWAYS);

        VBox pane = new VBox(12, toolbar, roomFilter, dayStrip, calendarRow);
        pane.getStyleClass().add("booking-pane");
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // Keyboard: left/right arrows for week nav
        pane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) { nextBtn.fire(); event.consume(); }
            if (event.getCode() == KeyCode.RIGHT) { prevBtn.fire(); event.consume(); }
        });

        // ---- Real-time polling ----
        startPolling(syncIndicator);

        return pane;
    }

    /** Legacy method — still supports standalone stage. */
    public void show(Stage stage) {
        stage.setTitle("ניהול הזמנות");
        Node content = createPane(stage);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        Scene scene = new Scene(scroll, 1200, 700);
        scene.getStylesheets().add(
                getClass().getClassLoader().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Stops the polling timer. Call this when the screen is destroyed.
     */
    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    /** If the server does not answer, the calendar keeps what it shows instead of showing an empty week. */
    private void reloadBookings() {
        ApiService.getInstance().loadBookings(weekStart, weekStart.plusDays(DAYS_IN_WEEK - 1)).ifPresent(fresh -> {
            bookings.clear();
            bookings.putAll(fresh);
        });
    }

    private Room roomNamed(String roomName) {
        return rooms.stream()
                .filter(room -> room.name().equals(roomName))
                .findFirst()
                .orElseThrow();
    }

    // ---- Rooms and their game times ----

    private record Schedule(List<Room> rooms, Map<String, List<LocalTime>> gameStarts) {
    }

    /** Network only, so it is safe to call off the FX thread. Empty if any call failed. */
    private Optional<Schedule> fetchSchedule() {
        return ApiService.getInstance().loadRooms().flatMap(loaded -> {
            Map<String, List<LocalTime>> starts = new HashMap<>();
            for (Room room : loaded) {
                Optional<List<LocalTime>> times = ApiService.getInstance().loadGameStartTimes(room.id(), LocalDate.now());
                if (times.isEmpty()) {
                    return Optional.empty();
                }
                starts.put(room.name(), times.get());
            }
            return Optional.of(new Schedule(loaded, starts));
        });
    }

    private boolean scheduleIsComplete() {
        return !rooms.isEmpty() && rooms.stream().allMatch(room -> gameStarts.containsKey(room.name()));
    }

    private void applySchedule(Schedule schedule) {
        rooms = schedule.rooms();
        gameStarts = schedule.gameStarts();
    }

    private void addRoomTabs() {
        roomFilter.getChildren().removeIf(node -> node instanceof ToggleButton tab && !ALL_ROOMS_LABEL.equals(tab.getText()));
        for (Room room : rooms) {
            ToggleButton tab = createRoomTab(room.name());
            tab.setToggleGroup(roomGroup);
            roomFilter.getChildren().add(tab);
        }
    }

    private void showSaveError(String reason) {
        Alert alert = new Alert(Alert.AlertType.ERROR, ServerMessages.inHebrew(reason));
        alert.setTitle(SAVE_FAILED_TITLE);
        alert.setHeaderText(null);
        if (ownerStage != null) alert.initOwner(ownerStage);
        alert.showAndWait();
    }

    // ================================================================
    // Real-time sync
    // ================================================================

    /** Shows the sync state as a dot and words, so it does not depend on telling two colors apart. */
    private void showSync(Label indicator, String text, Color dotColor) {
        indicator.setText(text);
        indicator.setGraphic(new Circle(SYNC_DOT_RADIUS, dotColor));
    }

    private void showSyncOk(Label indicator) {
        showSync(indicator, SYNC_OK_LABEL + LocalTime.now().format(timeFormat), SYNC_OK_COLOR);
    }

    private void startPolling(Label syncIndicator) {
        pollingTimeline = new Timeline(new KeyFrame(
                Duration.seconds(POLL_INTERVAL_SECONDS),
                event -> refreshFromServer(syncIndicator)
        ));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    /**
     * Fetches the latest bookings (and the room schedule, while it is still missing) in the background,
     * then rebuilds the calendar on the FX thread if something changed. A failed request changes nothing.
     */
    private void refreshFromServer(Label syncIndicator) {
        LocalDate requestedWeekStart = weekStart;
        boolean scheduleMissing = !scheduleIsComplete();
        new Thread(() -> {
            try {
                Optional<Schedule> schedule = scheduleMissing ? fetchSchedule() : Optional.empty();
                Optional<Map<String, Booking>> fresh = ApiService.getInstance()
                        .loadBookings(requestedWeekStart, requestedWeekStart.plusDays(DAYS_IN_WEEK - 1));
                Platform.runLater(() -> {
                    if (fresh.isEmpty()) {
                        showSync(syncIndicator, SYNC_FAILED_LABEL, SYNC_FAILED_COLOR);
                        return;
                    }
                    if (!requestedWeekStart.equals(weekStart)) {
                        return;
                    }
                    schedule.ifPresent(found -> {
                        applySchedule(found);
                        addRoomTabs();
                    });
                    if (schedule.isPresent() || !fresh.get().equals(bookings)) {
                        bookings.clear();
                        bookings.putAll(fresh.get());
                        rebuildCalendar();
                        showSyncOk(syncIndicator);
                        System.out.println("[Sync] Calendar updated with " + fresh.get().size() + " bookings.");
                    } else {
                        showSyncOk(syncIndicator);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showSync(syncIndicator, SYNC_FAILED_LABEL, SYNC_FAILED_COLOR));
                System.err.println("[Sync] Polling error: " + e.getMessage());
            }
        }).start();
    }

    // ================================================================
    // Calendar
    // ================================================================

    /** Shows another week; the chosen day becomes today if it is in that week, otherwise the week's first day. */
    private void goToWeek(LocalDate newWeekStart) {
        weekStart = newWeekStart;
        LocalDate today = LocalDate.now();
        boolean todayIsInWeek = !today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(DAYS_IN_WEEK - 1));
        selectedDay = todayIsInWeek ? today : weekStart;
        reloadBookings();
        rebuildCalendar();
    }

    /** Opens the week and day of a booking found by the search. */
    private void showBooking(Booking booking) {
        LocalDate day = booking.start().toLocalDate();
        goToWeek(day.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)));
        selectedDay = day;
        rebuildCalendar();
    }

    private void rebuildCalendar() {
        slotButtons.clear();

        LocalDate weekEnd = weekStart.plusDays(6);
        weekLabel.setText(
                weekStart.format(DateTimeFormatter.ofPattern("dd/MM")) + " - " +
                weekEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        boolean allRooms = selectedRoom == null;
        dayStrip.setVisible(allRooms);
        dayStrip.setManaged(allRooms);
        if (allRooms) {
            // ALL ROOMS mode: one day, columns = rooms, time running down
            rebuildDayStrip();
            buildAllRoomsView();
        } else {
            // SINGLE ROOM mode: columns = days, rows = time slots
            buildSingleRoomView();
        }
        overlapPanel.show(OverlapPanel.entries(bookings.values(), selectedRoom), this::dayLabel, this::showOverlapDay);
    }

    private String dayLabel(LocalDate date) {
        return "יום " + HEBREW_DAYS.get(date.getDayOfWeek()) + " " + date.format(DAY_CHIP_DATE_FORMAT);
    }

    /** The day timeline is where an overlap is shown in full, so a click on a message goes there. */
    private void showOverlapDay(LocalDate date) {
        selectedDay = date;
        if (selectedRoom == null) {
            rebuildCalendar();
        } else {
            allRoomsTab.setSelected(true);
        }
    }

    private void rebuildDayStrip() {
        dayStrip.getChildren().clear();
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            LocalDate date = weekStart.plusDays(i);
            long games = bookings.values().stream().filter(booking -> booking.start().toLocalDate().equals(date)).count();
            Button chip = new Button(HEBREW_DAYS.get(date.getDayOfWeek()) + "  " + date.format(DAY_CHIP_DATE_FORMAT)
                    + "\n" + gamesLabel(games));
            chip.getStyleClass().add("day-chip");
            if (date.equals(selectedDay)) chip.getStyleClass().add("day-chip-active");
            if (date.equals(LocalDate.now())) chip.getStyleClass().add("day-chip-today");
            chip.setAccessibleText(dayLabel(date));
            chip.setOnAction(e -> {
                selectedDay = date;
                rebuildCalendar();
            });
            dayStrip.getChildren().add(chip);
        }
    }

    private static String gamesLabel(long games) {
        if (games == 0) return NO_GAMES_LABEL;
        return games == 1 ? ONE_GAME_LABEL : games + GAMES_LABEL;
    }

    /** All-rooms view: the chosen day as a timeline with one column per room. */
    private void buildAllRoomsView() {
        List<Booking> dayBookings = bookings.values().stream()
                .filter(booking -> booking.start().toLocalDate().equals(selectedDay))
                .toList();
        scrollPane.setContent(DayTimelineView.build(rooms, gameStarts, dayBookings, selectedDay,
                this::createTimelineSlot, scrollPane));
    }

    private Button createTimelineSlot(LocalDate date, String room, LocalTime start) {
        LocalTime end = start.plusMinutes(roomNamed(room).durationMinutes());
        String timeRange = start.format(timeFormat) + "-" + end.format(timeFormat);
        String key = date.atTime(start).toString();
        Button slotBtn = createSlotButton(date, room, key, timeRange, true);
        slotButtons.put(key + "/" + room, slotBtn);
        return slotBtn;
    }

    /**
     * Single-room view: columns = days, rows = time slots for the selected room.
     */
    private void buildSingleRoomView() {
        HBox container = new HBox(8);
        container.getStyleClass().add("calendar-container");
        container.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            container.getChildren().add(buildDayColumn(date));
        }

        scrollPane.setContent(container);
    }

    private VBox buildDayColumn(LocalDate date) {
        VBox dayColumn = new VBox(6);
        dayColumn.getStyleClass().add("day-column");
        dayColumn.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        boolean isToday = date.equals(LocalDate.now());
        if (isToday) dayColumn.getStyleClass().add("day-column-today");
        Label dayLabel = new Label(
                HEBREW_DAYS.get(date.getDayOfWeek()) + "\n"
                + date.format(DateTimeFormatter.ofPattern("dd/MM"))
                + (isToday ? TODAY_SUFFIX : ""));
        dayLabel.getStyleClass().add("day-label");
        dayLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        dayColumn.getChildren().add(dayLabel);

        dayColumn.getChildren().add(buildRoomBox(date, selectedRoom));

        return dayColumn;
    }

    private VBox buildRoomBox(LocalDate date, String room) {
        VBox roomBox = new VBox(4);
        roomBox.getStyleClass().add("room-box");

        Label roomLabel = new Label(room);
        roomLabel.getStyleClass().add("room-label");
        roomBox.getChildren().add(roomLabel);

        int gameMinutes = roomNamed(room).durationMinutes();
        for (LocalTime start : gameStarts.getOrDefault(room, List.of())) {
            LocalTime end = start.plusMinutes(gameMinutes);
            String timeRange = start.format(timeFormat) + "-" + end.format(timeFormat);
            String key = date.atTime(start).toString();

            Button slotBtn = createSlotButton(date, room, key, timeRange, false);
            roomBox.getChildren().add(slotBtn);
            slotButtons.put(key + "/" + room, slotBtn);
        }

        return roomBox;
    }

    // ================================================================
    // Slot buttons
    // ================================================================

    /**
     * @param onTimeline true when the slot sits on the day timeline, where a shaded band already shows the overlap
     *                   and the slot has room for the booking type instead of an overlap note
     */
    private Button createSlotButton(LocalDate date, String room, String key, String timeRange, boolean onTimeline) {
        Button slotBtn = new Button(timeRange);
        slotBtn.setMinWidth(130);

        Booking existing = bookings.get(key + "/" + room);

        if (existing != null) {
            String overlapNote = overlapNote(existing);
            String shownNote = onTimeline ? "" : overlapNote;
            String typeLine = onTimeline && typeOf(existing) != BookingType.CLIENT ? "\n" + typeOf(existing) : "";
            BookingStatus status = BookingSearchDialog.statusOf(existing);
            typeLine += status == BookingStatus.CONFIRMED ? "" : "\n" + status;
            slotBtn.setText(timeRange + "\n" + existing.firstName() + " " + existing.lastName() + typeLine
                    + (shownNote.isEmpty() ? "" : "\n" + shownNote));
            // Color is persisted on the server — always use the server value
            String color = existing.color() != null ? existing.color() : "#fecaca";
            slotBtn.setStyle("-fx-background-color: " + color + ";");
            slotBtn.getStyleClass().add("slot-booked");
            if (!shownNote.isEmpty()) slotBtn.getStyleClass().add("slot-overlap");
            slotBtn.setTooltip(new Tooltip(
                    room + " | " + timeRange + "\n" +
                    existing.firstName() + " " + existing.lastName() + " (" + typeOf(existing) + ")\n" +
                    "משתתפים: " + existing.participants()
                    + (overlapNote.isEmpty() ? "" : "\n" + overlapNote)));
        } else {
            slotBtn.getStyleClass().add("slot-available");
            slotBtn.setTooltip(new Tooltip(room + " | " + timeRange + " — פנוי"));
        }

        slotBtn.setAccessibleText(room + " " + timeRange +
                (existing != null ? " הזמנה: " + existing.firstName() + " " + overlapNote(existing) : " פנוי"));

        String currentColor = (existing != null && existing.color() != null)
                ? existing.color() : "#fecaca";

        slotBtn.setOnAction(e -> openBookingModal(key, date, timeRange, slotBtn, room, currentColor));

        return slotBtn;
    }

    /**
     * Names the minutes this game shares with games in other rooms, one line per room, e.g. "⚠ חופף 19:00-19:15 · נרקוס".
     * Empty when it shares none. It compares real start and end times, so it stays right when rooms run different schedules.
     */
    private String overlapNote(Booking booking) {
        return bookings.values().stream()
                .filter(other -> !other.roomName().equals(booking.roomName()) && booking.overlaps(other))
                .sorted(Comparator.comparing(Booking::start))
                .map(other -> {
                    LocalDateTime sharedStart = booking.start().isAfter(other.start()) ? booking.start() : other.start();
                    LocalDateTime sharedEnd = booking.end().isBefore(other.end()) ? booking.end() : other.end();
                    return OVERLAP_PREFIX + sharedStart.format(timeFormat) + "-" + sharedEnd.format(timeFormat)
                            + " · " + other.roomName();
                })
                .collect(Collectors.joining("\n"));
    }

    // Bookings made before the type existed have none in old server answers.
    private static BookingType typeOf(Booking booking) {
        return booking.type() != null ? booking.type() : BookingType.CLIENT;
    }

    /** One round swatch per booking color. The chosen color goes into {@code selectedColor}. */
    private HBox colorSwatches(String currentColor, String[] selectedColor) {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(8);
        for (int i = 0; i < SWATCH_COLORS.size(); i++) {
            String color = SWATCH_COLORS.get(i);
            ToggleButton swatch = new ToggleButton();
            swatch.getStyleClass().add("color-swatch");
            swatch.setStyle("-fx-background-color: " + color + ";");
            swatch.setUserData(color);
            swatch.setToggleGroup(group);
            swatch.setSelected(color.equals(currentColor));
            swatch.setTooltip(new Tooltip(SWATCH_NAMES.get(i)));
            swatch.setAccessibleText("צבע " + SWATCH_NAMES.get(i));
            row.getChildren().add(swatch);
        }
        // Clicking the chosen swatch again must not leave the booking with no color.
        group.selectedToggleProperty().addListener((obs, previous, chosen) -> {
            if (chosen == null) {
                previous.setSelected(true);
            } else {
                selectedColor[0] = (String) chosen.getUserData();
            }
        });
        return row;
    }

    private boolean confirmCancel(Booking booking) {
        ButtonType yes = new ButtonType(CANCEL_CONFIRM_YES, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType(CANCEL_CONFIRM_NO, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, CANCEL_CONFIRM_MESSAGE.formatted(
                booking.firstName(), booking.lastName(), booking.roomName(), booking.start().format(CANCEL_DATE_FORMAT)), yes, no);
        alert.setTitle(CANCEL_CONFIRM_TITLE);
        alert.setHeaderText(null);
        if (ownerStage != null) alert.initOwner(ownerStage);
        return alert.showAndWait().filter(yes::equals).isPresent();
    }

    private ToggleButton createRoomTab(String text) {
        ToggleButton tab = new ToggleButton(text);
        tab.getStyleClass().add("room-tab");
        tab.setTooltip(new Tooltip("סנן לפי: " + text));
        tab.setAccessibleText("סינון חדר: " + text);
        return tab;
    }

    // ================================================================
    // Booking modal
    // ================================================================

    private void openBookingModal(String key, LocalDate date, String timeRange,
                                   Button slotBtn, String roomName, String currentColor) {
        Booking existing = bookings.get(key + "/" + roomName);

        Dialog<ButtonType> dialog = new Dialog<>();
        if (ownerStage != null) dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "הזמנת משחק חדש" : "פרטי הזמנה");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("booking-form");
        grid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Label summary = new Label("חדר: " + roomName + "     תאריך: יום " + HEBREW_DAYS.get(date.getDayOfWeek())
                + " " + date.format(MODAL_DATE_FORMAT) + "     שעה: " + timeRange);
        summary.getStyleClass().add("booking-summary");
        summary.setMaxWidth(Double.MAX_VALUE);

        TextField nameField = new TextField();
        nameField.setAccessibleText("שם פרטי");

        ComboBox<BookingType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(BookingType.values());
        typeBox.setValue(BookingType.CLIENT);
        typeBox.setAccessibleText("סוג הזמנה");
        typeBox.setTooltip(new Tooltip("סוג ההזמנה"));
        HBox nameAndType = new HBox(8, nameField, typeBox);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        TextField lastNameField = new TextField();
        lastNameField.setAccessibleText("שם משפחה");
        TextField phoneField = new TextField();
        phoneField.setAccessibleText("טלפון");
        TextField emailField = new TextField();
        emailField.setAccessibleText("אימייל");

        ComboBox<Integer> participantsBox = new ComboBox<>();
        for (int count = MIN_STAFF_PARTICIPANTS; count <= roomNamed(roomName).capacity(); count++) {
            participantsBox.getItems().add(count);
        }
        participantsBox.setValue(2);
        participantsBox.setAccessibleText("מספר משתתפים");

        ComboBox<String> experienceBox = new ComboBox<>();
        experienceBox.getItems().addAll("מתחילים", "מנוסים");
        experienceBox.setValue("מתחילים");
        experienceBox.setAccessibleText("רמת ניסיון");

        ComboBox<BookingStatus> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.NO_SHOW);
        statusBox.setValue(BookingStatus.CONFIRMED);
        statusBox.setAccessibleText("סטטוס הזמנה");

        // What the client wrote when booking. Staff read it but write their own notes below.
        TextArea clientNotesArea = new TextArea();
        clientNotesArea.setPrefRowCount(2);
        clientNotesArea.setEditable(false);
        clientNotesArea.setFocusTraversable(false);
        clientNotesArea.getStyleClass().add("read-only-notes");
        clientNotesArea.setAccessibleText("הערות הלקוח, לקריאה בלבד");

        TextArea internalNotesArea = new TextArea();
        internalNotesArea.setPrefRowCount(3);
        internalNotesArea.setPromptText("נראה רק לצוות");
        internalNotesArea.setAccessibleText("הערות פנימיות");

        if (existing != null) {
            nameField.setText(existing.firstName());
            lastNameField.setText(existing.lastName());
            phoneField.setText(existing.phoneNumber());
            emailField.setText(existing.email());
            participantsBox.setValue(existing.participants());
            experienceBox.setValue(existing.experience());
            typeBox.setValue(typeOf(existing));
            statusBox.setValue(BookingSearchDialog.statusOf(existing));
            clientNotesArea.setText(existing.notes());
            internalNotesArea.setText(existing.internalNotes());
        }

        grid.add(summary, 0, 0, 2, 1);
        grid.add(new Label("שם פרטי וסוג:"), 0, 1);     grid.add(nameAndType, 1, 1);
        grid.add(new Label("שם משפחה:"), 0, 2);        grid.add(lastNameField, 1, 2);
        grid.add(new Label("טלפון:"), 0, 3);           grid.add(phoneField, 1, 3);
        grid.add(new Label("אימייל:"), 0, 4);           grid.add(emailField, 1, 4);
        grid.add(new Label("מספר משתתפים:"), 0, 5);     grid.add(participantsBox, 1, 5);
        grid.add(new Label("רמת ניסיון:"), 0, 6);       grid.add(experienceBox, 1, 6);
        // A booking that staff are creating has no client notes yet, so the row would only be an empty box.
        if (existing != null) {
            grid.add(new Label("הערות הלקוח:"), 0, 7);  grid.add(clientNotesArea, 1, 7);
        }
        if (existing != null) {
            grid.add(new Label("סטטוס:"), 0, 11);       grid.add(statusBox, 1, 11);
        }
        grid.add(new Label("הערות פנימיות:"), 0, 8);    grid.add(internalNotesArea, 1, 8);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("שמור", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveType,
                new ButtonType("בטל את ההזמנה", ButtonBar.ButtonData.OTHER),
                new ButtonType("סגור", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        // Use existing color from server, not a default; the choice is saved with the rest of the form.
        final String[] selectedColor = {currentColor};
        grid.add(new Label("צבע בלוח:"), 0, 9);
        grid.add(colorSwatches(currentColor, selectedColor), 1, 9);

        // Shown when Save is pressed with a wrong field; the dialog stays open instead of the server refusing later.
        Label formError = new Label();
        formError.getStyleClass().add("form-error");
        grid.add(formError, 1, 10);
        FormChecks.guardSave(dialog, saveType, nameField, lastNameField, phoneField, emailField, formError);

        dialog.setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Client client = new Client(null, nameField.getText(), lastNameField.getText(),
                        phoneField.getText(), emailField.getText());
                BookingDetails details = new BookingDetails(participantsBox.getValue(), experienceBox.getValue(),
                        existing == null ? null : existing.notes(), selectedColor[0],
                        typeBox.getValue(), internalNotesArea.getText());

                Optional<String> failure = existing == null
                        ? ApiService.getInstance().addBooking(
                                new CreateBookingRequest(roomNamed(roomName).id(), key, client, details))
                        : ApiService.getInstance().updateBooking(existing.id(), new UpdateBookingRequest(client, details));
                failure.ifPresent(this::showSaveError);
                if (failure.isEmpty() && existing != null && statusBox.getValue() != BookingSearchDialog.statusOf(existing)) {
                    ApiService.getInstance().updateBookingStatus(existing.id(), statusBox.getValue()).ifPresent(this::showSaveError);
                }
                reloadBookings();
                rebuildCalendar();
            }

            if (button.getButtonData() == ButtonBar.ButtonData.OTHER && existing != null && confirmCancel(existing)) {
                ApiService.getInstance().deleteBooking(existing.id()).ifPresent(this::showSaveError);
                reloadBookings();
                rebuildCalendar();
            }

            return button;
        });

        dialog.showAndWait();
    }
}
