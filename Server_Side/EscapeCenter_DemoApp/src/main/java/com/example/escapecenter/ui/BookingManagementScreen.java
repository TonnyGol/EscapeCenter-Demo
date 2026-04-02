package com.example.escapecenter.ui;

import com.example.escapecenter.model.Booking;
import com.example.escapecenter.service.ApiService;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Booking management screen — weekly calendar grid with room filter tabs,
 * week navigation, real-time polling, and dual layout modes.
 *
 * <ul>
 *   <li><b>Single room</b>: columns = days, rows = time slots (classic view)</li>
 *   <li><b>All rooms ("הכל")</b>: columns = rooms, with day+time rows nested inside
 *       so you can see parallel games across different rooms</li>
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

    private static final String[] ROOMS = {"אחוזת השכן", "מקדש הקאמי", "ההתערבות", "אינפיניטי", "נרקוס"};
    private static final int APPOINTMENT_DURATION = 90;
    private static final int BREAK_DURATION = 30;
    private static final int APPOINTMENTS_PER_DAY = 7;
    private static final LocalTime START_TIME = LocalTime.of(10, 0);

    /** Polling interval in seconds for real-time sync. */
    private static final int POLL_INTERVAL_SECONDS = 10;

    private LocalDate weekStart;
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

        // Load bookings from server
        bookings.clear();
        bookings.putAll(ApiService.getInstance().loadBookings());

        // Start on current week (Sunday of this week)
        weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        // ---- Toolbar: Week navigation ----
        Button prevBtn = new Button("◀ שבוע קודם");
        prevBtn.getStyleClass().add("week-nav-btn");
        prevBtn.setTooltip(new Tooltip("חזור שבוע אחורה"));
        prevBtn.setAccessibleText("שבוע קודם");

        Button nextBtn = new Button("שבוע הבא ▶");
        nextBtn.getStyleClass().add("week-nav-btn");
        nextBtn.setTooltip(new Tooltip("עבור לשבוע הבא"));
        nextBtn.setAccessibleText("שבוע הבא");

        Button todayBtn = new Button("📍 היום");
        todayBtn.getStyleClass().add("week-nav-btn");
        todayBtn.setTooltip(new Tooltip("חזור לשבוע הנוכחי"));
        todayBtn.setAccessibleText("חזור לשבוע הנוכחי");

        weekLabel = new Label();
        weekLabel.getStyleClass().add("week-label");

        Label syncIndicator = new Label("🔄");
        syncIndicator.getStyleClass().add("sync-indicator");
        syncIndicator.setTooltip(new Tooltip("סנכרון אוטומטי כל " + POLL_INTERVAL_SECONDS + " שניות"));

        prevBtn.setOnAction(e -> { weekStart = weekStart.minusWeeks(1); rebuildCalendar(); });
        nextBtn.setOnAction(e -> { weekStart = weekStart.plusWeeks(1); rebuildCalendar(); });
        todayBtn.setOnAction(e -> { weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)); rebuildCalendar(); });

        HBox toolbar = new HBox(10, nextBtn, weekLabel, prevBtn, todayBtn, syncIndicator);
        toolbar.getStyleClass().add("booking-toolbar");
        toolbar.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // ---- Room Filter Tabs ----
        HBox roomFilter = new HBox(8);
        roomFilter.getStyleClass().add("room-filter-bar");
        roomFilter.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        ToggleButton allRoomsTab = createRoomTab("הכל");
        allRoomsTab.setSelected(true);
        allRoomsTab.getStyleClass().add("room-tab-active");

        ToggleGroup roomGroup = new ToggleGroup();
        allRoomsTab.setToggleGroup(roomGroup);
        roomFilter.getChildren().add(allRoomsTab);

        for (String room : ROOMS) {
            ToggleButton tab = createRoomTab(room);
            tab.setToggleGroup(roomGroup);
            roomFilter.getChildren().add(tab);
        }

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
            selectedRoom = text.equals("הכל") ? null : text;
            rebuildCalendar();
        });

        // ---- Calendar Container ----
        calendarContainer = new HBox(8);
        calendarContainer.getStyleClass().add("calendar-container");
        ((HBox) calendarContainer).setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        scrollPane = new ScrollPane(calendarContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rebuildCalendar();

        VBox pane = new VBox(12, toolbar, roomFilter, scrollPane);
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

    // ================================================================
    // Real-time sync
    // ================================================================

    private void startPolling(Label syncIndicator) {
        pollingTimeline = new Timeline(new KeyFrame(
                Duration.seconds(POLL_INTERVAL_SECONDS),
                event -> refreshFromServer(syncIndicator)
        ));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    /**
     * Fetches latest bookings from server in background, then rebuilds
     * the calendar on the FX thread if data changed.
     */
    private void refreshFromServer(Label syncIndicator) {
        new Thread(() -> {
            try {
                Map<String, Booking> fresh = ApiService.getInstance().loadBookings();
                Platform.runLater(() -> {
                    if (!fresh.equals(bookings)) {
                        bookings.clear();
                        bookings.putAll(fresh);
                        rebuildCalendar();
                        syncIndicator.setText("🟢");
                        System.out.println("[Sync] Calendar updated with " + fresh.size() + " bookings.");
                    } else {
                        syncIndicator.setText("🔄");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> syncIndicator.setText("🔴"));
                System.err.println("[Sync] Polling error: " + e.getMessage());
            }
        }).start();
    }

    // ================================================================
    // Calendar
    // ================================================================

    private void rebuildCalendar() {
        slotButtons.clear();

        LocalDate weekEnd = weekStart.plusDays(6);
        weekLabel.setText(
                weekStart.format(DateTimeFormatter.ofPattern("dd/MM")) + " - " +
                weekEnd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        if (selectedRoom == null) {
            // ALL ROOMS mode: columns = rooms, rows = days × time slots
            buildAllRoomsView();
        } else {
            // SINGLE ROOM mode: columns = days, rows = time slots
            buildSingleRoomView();
        }
    }

    /**
     * All-rooms view: each column is a ROOM, each row within is a day header
     * followed by time slots. This lets you see parallel games across rooms.
     */
    private void buildAllRoomsView() {
        HBox container = new HBox(8);
        container.getStyleClass().add("calendar-container");
        container.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        for (String room : ROOMS) {
            VBox roomColumn = new VBox(4);
            roomColumn.getStyleClass().add("day-column");
            roomColumn.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            roomColumn.setMinWidth(160);

            // Room header
            Label roomHeader = new Label("🚪 " + room);
            roomHeader.getStyleClass().add("room-column-header");
            roomColumn.getChildren().add(roomHeader);

            // For each day in the week
            for (int i = 0; i < 7; i++) {
                LocalDate date = weekStart.plusDays(i);
                boolean isToday = date.equals(LocalDate.now());

                // Day sub-header
                Label dayLabel = new Label(
                        HEBREW_DAYS.get(date.getDayOfWeek()) + " " +
                        date.format(DateTimeFormatter.ofPattern("dd/MM")) +
                        (isToday ? " 📍" : ""));
                dayLabel.getStyleClass().add("day-sub-header");
                roomColumn.getChildren().add(dayLabel);

                // Time slots for this room on this day
                for (int j = 0; j < APPOINTMENTS_PER_DAY; j++) {
                    LocalTime start = START_TIME.plusMinutes((long) j * (APPOINTMENT_DURATION + BREAK_DURATION));
                    LocalTime end = start.plusMinutes(APPOINTMENT_DURATION);
                    String timeRange = start.format(timeFormat) + "-" + end.format(timeFormat);
                    String key = date.format(DateTimeFormatter.ofPattern("dd.MM")) + "/" + timeRange;

                    Button slotBtn = createSlotButton(date, room, key, timeRange);
                    roomColumn.getChildren().add(slotBtn);
                    slotButtons.put(key + "/" + room, slotBtn);
                }

                // Small spacer between days
                Region daySpacer = new Region();
                daySpacer.setMinHeight(6);
                roomColumn.getChildren().add(daySpacer);
            }

            container.getChildren().add(roomColumn);
        }

        scrollPane.setContent(container);
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
        Label dayLabel = new Label(
                HEBREW_DAYS.get(date.getDayOfWeek()) + "\n"
                + date.format(DateTimeFormatter.ofPattern("dd/MM"))
                + (isToday ? " 📍" : ""));
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

        for (int j = 0; j < APPOINTMENTS_PER_DAY; j++) {
            LocalTime start = START_TIME.plusMinutes((long) j * (APPOINTMENT_DURATION + BREAK_DURATION));
            LocalTime end = start.plusMinutes(APPOINTMENT_DURATION);
            String timeRange = start.format(timeFormat) + "-" + end.format(timeFormat);
            String key = date.format(DateTimeFormatter.ofPattern("dd.MM")) + "/" + timeRange;

            Button slotBtn = createSlotButton(date, room, key, timeRange);
            roomBox.getChildren().add(slotBtn);
            slotButtons.put(key + "/" + room, slotBtn);
        }

        return roomBox;
    }

    // ================================================================
    // Slot buttons
    // ================================================================

    private Button createSlotButton(LocalDate date, String room, String key, String timeRange) {
        Button slotBtn = new Button(timeRange);
        slotBtn.setMinWidth(130);

        Booking existing = bookings.get(key + "/" + room);

        if (existing != null) {
            slotBtn.setText(timeRange + "\n" + existing.firstName() + " " + existing.lastName());
            // Color is persisted on the server — always use the server value
            String color = existing.color() != null ? existing.color() : "#fecaca";
            slotBtn.setStyle("-fx-background-color: " + color + ";");
            slotBtn.getStyleClass().add("slot-booked");
            slotBtn.setTooltip(new Tooltip(
                    room + " | " + timeRange + "\n" +
                    existing.firstName() + " " + existing.lastName() + "\n" +
                    "משתתפים: " + existing.participants()));
        } else {
            slotBtn.getStyleClass().add("slot-available");
            slotBtn.setTooltip(new Tooltip(room + " | " + timeRange + " — פנוי"));
        }

        slotBtn.setAccessibleText(room + " " + timeRange +
                (existing != null ? " הזמנה: " + existing.firstName() : " פנוי"));

        String currentColor = (existing != null && existing.color() != null)
                ? existing.color() : "#fecaca";

        slotBtn.setOnAction(e -> openBookingModal(key, date, timeRange, slotBtn, room, currentColor));

        return slotBtn;
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

        TextField nameField = new TextField();
        nameField.setAccessibleText("שם פרטי");
        TextField lastNameField = new TextField();
        lastNameField.setAccessibleText("שם משפחה");
        TextField phoneField = new TextField();
        phoneField.setAccessibleText("טלפון");
        TextField emailField = new TextField();
        emailField.setAccessibleText("אימייל");

        ComboBox<Integer> participantsBox = new ComboBox<>();
        participantsBox.getItems().addAll(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        participantsBox.setValue(2);
        participantsBox.setAccessibleText("מספר משתתפים");

        ComboBox<String> experienceBox = new ComboBox<>();
        experienceBox.getItems().addAll("מתחילים", "מנוסים");
        experienceBox.setValue("מתחילים");
        experienceBox.setAccessibleText("רמת ניסיון");

        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setAccessibleText("הערות");

        if (existing != null) {
            nameField.setText(existing.firstName());
            lastNameField.setText(existing.lastName());
            phoneField.setText(existing.phoneNumber());
            emailField.setText(existing.email());
            participantsBox.setValue(existing.participants());
            experienceBox.setValue(existing.experience());
            notesArea.setText(existing.notes());
        }

        grid.add(new Label("שם פרטי:"), 0, 0);        grid.add(nameField, 1, 0);
        grid.add(new Label("שם משפחה:"), 0, 1);        grid.add(lastNameField, 1, 1);
        grid.add(new Label("טלפון:"), 0, 2);           grid.add(phoneField, 1, 2);
        grid.add(new Label("אימייל:"), 0, 3);           grid.add(emailField, 1, 3);
        grid.add(new Label("מספר משתתפים:"), 0, 4);     grid.add(participantsBox, 1, 4);
        grid.add(new Label("רמת ניסיון:"), 0, 5);       grid.add(experienceBox, 1, 5);
        grid.add(new Label("הערות:"), 0, 6);             grid.add(notesArea, 1, 6);

        dialog.getDialogPane().setContent(grid);

        ButtonType colorBtn = new ButtonType("שנה צבע", ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().addAll(
                colorBtn,
                new ButtonType("שמור", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("בטל הזמנה", ButtonBar.ButtonData.OTHER),
                new ButtonType("סגור", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        // Use existing color from server, not a default
        final String[] selectedColor = {currentColor};

        Button changeColorNode = (Button) dialog.getDialogPane().lookupButton(colorBtn);
        changeColorNode.setTooltip(new Tooltip("שנה את צבע ההזמנה בלוח"));
        changeColorNode.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            List<String> colorNames = Arrays.asList("אדום", "ירוק", "צהוב", "כחול", "כחלת");
            List<String> bgColors = Arrays.asList("#fecaca", "#d1fae5", "#fef3c7", "#c7d2fe", "#e0e7ff");

            ChoiceDialog<String> colorDialog = new ChoiceDialog<>("", colorNames);
            colorDialog.setTitle("בחר צבע חדש");
            colorDialog.setHeaderText("בחר צבע להזמנה");

            colorDialog.showAndWait().ifPresent(name -> {
                int idx = colorNames.indexOf(name);
                if (idx >= 0) {
                    selectedColor[0] = bgColors.get(idx);
                    slotBtn.setStyle("-fx-background-color: " + selectedColor[0] + ";");

                    // Immediately persist color change to server for existing bookings
                    Booking current = bookings.get(key + "/" + roomName);
                    if (current != null) {
                        Booking updated = new Booking(
                                current.bookingID(), current.room(),
                                current.firstName(), current.lastName(),
                                current.phoneNumber(), current.email(),
                                current.experience(), current.notes(),
                                current.participants(), selectedColor[0]
                        );
                        bookings.put(key + "/" + roomName, updated);
                        ApiService.getInstance().updateBooking(updated);
                        System.out.println("[BookingScreen] Color updated and saved: " + selectedColor[0]);
                    }
                }
            });
        });

        dialog.setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                // Color is saved as part of the Booking and persisted on the server
                Booking booking = new Booking(
                        key, roomName,
                        nameField.getText(), lastNameField.getText(),
                        phoneField.getText(), emailField.getText(),
                        experienceBox.getValue(), notesArea.getText(),
                        participantsBox.getValue(), selectedColor[0]
                );
                bookings.put(key + "/" + roomName, booking);

                if (existing == null) {
                    ApiService.getInstance().addBooking(booking);
                } else {
                    ApiService.getInstance().updateBooking(booking);
                }

                slotBtn.setText(timeRange + "\n" + booking.firstName() + " " + booking.lastName());
                slotBtn.setStyle("-fx-background-color: " + selectedColor[0] + ";");
                slotBtn.getStyleClass().remove("slot-available");
                if (!slotBtn.getStyleClass().contains("slot-booked")) {
                    slotBtn.getStyleClass().add("slot-booked");
                }
            }

            if (button.getButtonData() == ButtonBar.ButtonData.OTHER && existing != null) {
                ApiService.getInstance().deleteBooking(existing.bookingID(), existing.room());
                bookings.remove(key + "/" + roomName);

                slotBtn.setText(timeRange);
                slotBtn.getStyleClass().removeAll("slot-booked");
                slotBtn.getStyleClass().add("slot-available");
                slotBtn.setStyle("");
            }

            return button;
        });

        dialog.showAndWait();
    }
}
