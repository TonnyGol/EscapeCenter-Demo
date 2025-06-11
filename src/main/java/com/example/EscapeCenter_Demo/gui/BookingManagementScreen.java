package com.example.EscapeCenter_Demo.gui;

import com.example.EscapeCenter_Demo.Booking;
import com.example.EscapeCenter_Demo.DataBaseService.BookingService;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class BookingManagementScreen {

    private final Map<String, Booking> bookings = new HashMap<>();
    private final Map<String, Button> slotButtons = new HashMap<>();
    private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<DayOfWeek, String> hebrewDays = Map.of(
            DayOfWeek.SUNDAY, "ראשון",
            DayOfWeek.MONDAY, "שני",
            DayOfWeek.TUESDAY, "שלישי",
            DayOfWeek.WEDNESDAY, "רביעי",
            DayOfWeek.THURSDAY, "חמישי",
            DayOfWeek.FRIDAY, "שישי",
            DayOfWeek.SATURDAY, "שבת"
    );


    private final int appointmentDuration = 90;
    private final int breakDuration = 30;
    private final int appointmentsPerDay = 7;
    private final LocalTime startTime = LocalTime.of(10, 0);

    public void start(Stage stage) {
        stage.setTitle("ניהול הזמנות");

        HBox weekView = new HBox(10);
        weekView.setPadding(new Insets(20));
        weekView.setAlignment(Pos.TOP_CENTER);
        weekView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        LocalDate today = LocalDate.now();
        //LocalDate weekStart = today.with(DayOfWeek.SUNDAY);
        LocalDate weekStart = today.with(DayOfWeek.SUNDAY).minusWeeks(1);

        bookings.putAll(BookingService.getAllBookings());

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            VBox dayColumn = new VBox(10);
            dayColumn.setAlignment(Pos.TOP_CENTER);
            dayColumn.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            Label dayLabel = new Label(hebrewDays.get(date.getDayOfWeek()) + "\n" + date.format(DateTimeFormatter.ofPattern("dd/MM")));
            dayLabel.setStyle("-fx-font-weight: bold;");
            dayColumn.getChildren().add(dayLabel);

            for (int j = 0; j < appointmentsPerDay; j++) {
                LocalTime start = startTime.plusMinutes(j * (appointmentDuration + breakDuration));
                LocalTime end = start.plusMinutes(appointmentDuration);
                String timeRange = start.format(timeFormat) + "-" + end.format(timeFormat);
                String key = date.format(DateTimeFormatter.ofPattern("dd.MM")) + "/" + timeRange;

                Button slotBtn = new Button(timeRange);
                slotBtn.setMinWidth(120);

                Booking existingBooking = bookings.get(key);
                if (existingBooking != null) {
                    slotBtn.setText(timeRange + "\n" + existingBooking.getFirstName() + " " + existingBooking.getLastName());
                    slotBtn.setStyle("-fx-background-color: #fecaca; -fx-text-fill: #991b1b;");
                } else {
                    slotBtn.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
                }

                slotBtn.setOnAction(e -> openBookingModal(stage, key, date, timeRange, slotBtn));
                dayColumn.getChildren().add(slotBtn);

                slotButtons.put(key, slotBtn);
            }

            weekView.getChildren().add(dayColumn);
        }

        ScrollPane scrollPane = new ScrollPane(weekView);
        scrollPane.setFitToWidth(true);

        stage.setScene(new Scene(scrollPane, 950, 600));
        stage.show();
    }

    private void openBookingModal(Stage parent, String key, LocalDate date, String timeRange, Button slotBtn) {
        Booking existing = bookings.get(key);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(parent);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "הזמנת משחק חדש" : "פרטי הזמנה");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        TextField nameField = new TextField();
        TextField lastNameField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();
        ComboBox<Integer> participantsBox = new ComboBox<>();
        participantsBox.getItems().addAll(2,3,4,5,6,7,8,9,10,11,12);
        participantsBox.setValue(2);
        ComboBox<String> experienceBox = new ComboBox<>();
        experienceBox.getItems().addAll("מתחילים", "מנוסים");
        experienceBox.setValue("מתחילים");
        TextArea notesArea = new TextArea();

        if (existing != null) {
            nameField.setText(existing.getFirstName());
            lastNameField.setText(existing.getLastName());
            phoneField.setText(existing.getPhoneNumber());
            emailField.setText(existing.getEmail());
            participantsBox.setValue(existing.getParticipants());
            experienceBox.setValue(existing.getExperience());
            notesArea.setText(existing.getNotes());
        }

        grid.add(new Label("שם פרטי:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("שם משפחה:"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(new Label("טלפון:"), 0, 2); grid.add(phoneField, 1, 2);
        grid.add(new Label("אימייל:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("מספר משתתפים:"), 0, 4); grid.add(participantsBox, 1, 4);
        grid.add(new Label("רמת ניסיון:"), 0, 5); grid.add(experienceBox, 1, 5);
        grid.add(new Label("הערות:"), 0, 6); grid.add(notesArea, 1, 6);

        dialog.getDialogPane().setContent(grid);

        if (existing == null) {
            dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("אישור", ButtonBar.ButtonData.OK_DONE), new ButtonType("ביטול", ButtonBar.ButtonData.CANCEL_CLOSE));
        } else {
            ButtonType editButton = new ButtonType("ערוך", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBookingButton = new ButtonType("בטל הזמנה", ButtonBar.ButtonData.OTHER);
            dialog.getDialogPane().getButtonTypes().addAll(new ButtonType("סגור", ButtonBar.ButtonData.CANCEL_CLOSE), editButton, cancelBookingButton);
        }

        dialog.setResultConverter(button -> {
            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Booking booking = new Booking(
                        key,
                        nameField.getText(),
                        lastNameField.getText(),
                        phoneField.getText(),
                        emailField.getText(),
                        experienceBox.getValue(),
                        notesArea.getText(),
                        participantsBox.getValue()
                );
                bookings.put(key, booking);
                if (existing == null) {
                    BookingService.addBooking(booking);
                } else {
                    BookingService.updateBooking(booking);
                }
                slotBtn.setText(timeRange + "\n" + booking.getFirstName() + " " + booking.getLastName());
                slotBtn.setStyle("-fx-background-color: #fecaca; -fx-text-fill: #991b1b;");
            } else if (button.getButtonData() == ButtonBar.ButtonData.OTHER) {
                if (existing != null) {
                    BookingService.deleteBooking(existing.getBookingID());
                    bookings.remove(key);
                }
                slotBtn.setText(timeRange);
                slotBtn.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
            }
            return button;
        });

        dialog.showAndWait();
    }
}
