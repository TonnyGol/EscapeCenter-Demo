package com.example.EscapeCenter_Demo.gui;

import com.example.EscapeCenter_Demo.Booking;
import com.example.EscapeCenter_Demo.DataBaseService.BookingService;
import jakarta.mail.MessagingException;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BookingManagementScreen {

    private final Map<String, Booking> bookings = new HashMap<>();
    private final Map<String, Button> slotButtons = new HashMap<>();
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
        weekView.setSpacing(30);
        weekView.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.SUNDAY).minusWeeks(1);

        bookings.putAll(BookingService.getAllBookings());

        String[] rooms = {"אחוזת השכן", "מקדש הקאמי", "ההתערבות", "אינפיניטי", "נרקוס"};

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);

            VBox dayColumn = new VBox(10);
            dayColumn.setAlignment(Pos.TOP_CENTER);
            dayColumn.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            Label dayLabel = new Label(hebrewDays.get(date.getDayOfWeek()) + "\n" + date.format(DateTimeFormatter.ofPattern("dd/MM")));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15;");
            dayColumn.getChildren().add(dayLabel);

            for (String room : rooms) {
                VBox roomBox = new VBox(5);
                roomBox.setAlignment(Pos.TOP_CENTER);

                Label roomLabel = new Label(room);
                roomLabel.setStyle("-fx-underline: true; -fx-font-weight: bold; -fx-font-size: 15;");
                roomBox.getChildren().add(roomLabel);

                for (int j = 0; j < appointmentsPerDay; j++) {
                    LocalTime start = startTime.plusMinutes(j * (appointmentDuration + breakDuration));
                    LocalTime end = start.plusMinutes(appointmentDuration);
                    String timeRange = start.format(timeFormat) + "-" + end.format(timeFormat);

                    String key = date.format(DateTimeFormatter.ofPattern("dd.MM")) + "/" + timeRange;
                    Button slotBtn = new Button(timeRange);
                    slotBtn.setMinWidth(110);

                    Booking existingBooking = bookings.get(key+"/"+room);
                    String bookingColor = "#fecaca";

                    if (existingBooking != null) {
                        slotBtn.setText(timeRange + "\n" + existingBooking.getFirstName() + " " + existingBooking.getLastName());
                        bookingColor = existingBooking.getColor();
                        slotBtn.setStyle("-fx-background-color: " + bookingColor + "; -fx-text-fill: #000000; -fx-font-size: 13;");
                    } else {
                        slotBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #000000; -fx-font-size: 13;");
                    }

                    String finalColor = bookingColor;
                    slotBtn.setOnAction(e -> openBookingModal(stage, key, date, timeRange, slotBtn, room, finalColor));
                    roomBox.getChildren().add(slotBtn);
                    slotButtons.put(key, slotBtn);
                }
                dayColumn.getChildren().add(roomBox);
            }
            weekView.getChildren().add(dayColumn);
        }

        ScrollPane scrollPane = new ScrollPane(weekView);
        scrollPane.setFitToWidth(true);

        stage.setScene(new Scene(scrollPane, 1200, 700));
        stage.show();
    }

    private void openBookingModal(Stage parent, String key, LocalDate date, String timeRange, Button slotBtn, String roomName, String currentColor) {
        Booking existing = bookings.get(key+"/"+roomName);

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
        ButtonType colorBtn = new ButtonType("שנה צבע", ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().addAll(
                colorBtn,
                new ButtonType("שמור", ButtonBar.ButtonData.OK_DONE),
                new ButtonType("בטל הזמנה", ButtonBar.ButtonData.OTHER),
                new ButtonType("סגור", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        final String[] selectedColor = {currentColor};

        Button changeColorNode = (Button) dialog.getDialogPane().lookupButton(colorBtn);
        changeColorNode.addEventFilter(ActionEvent.ACTION, event -> {
            // Prevent the dialog from closing
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
                    slotBtn.setStyle("-fx-background-color: " + selectedColor[0] + "; -fx-text-fill: #000000;");
                }
            });
        });

        dialog.setResultConverter(button -> {

            if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Booking booking = new Booking(
                        key, roomName,
                        nameField.getText(), lastNameField.getText(),
                        phoneField.getText(), emailField.getText(),
                        experienceBox.getValue(), notesArea.getText(),
                        participantsBox.getValue(), selectedColor[0]
                );
                bookings.put(key+"/"+roomName, booking);

                if (existing == null) {
                    BookingService.addBooking(booking);
                    EmailService.sendBookMail(booking,
                            date.format(DateTimeFormatter.ofPattern("dd-MM")), timeRange);
                } else {
                    BookingService.updateBooking(booking);
                }

                slotBtn.setText(timeRange + "\n" + booking.getFirstName() + " " + booking.getLastName());
                slotBtn.setStyle("-fx-background-color: " + selectedColor[0] + "; -fx-text-fill: #000000;");
            }

            if (button.getButtonData() == ButtonBar.ButtonData.OTHER && existing != null) {
                BookingService.deleteBooking(existing.getBookingID(), existing.getRoom());
                bookings.remove(key+"/"+roomName);
                sendCancelBookMail(existing,
                        date.format(DateTimeFormatter.ofPattern("dd-MM")), timeRange);

                slotBtn.setText(timeRange);
                slotBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #000000;");
            }

            return button;
        });

        dialog.showAndWait();
    }

    private void sendCancelBookMail(Booking existing, String date, String timeRange) {
        try {
            String message = String.format("""
                        שלום %s %s,

                        ההזמנה שלך בוטלה בהצלחה.

                        פרטי ההזמנה שבוטלה:
                        ---------------------------
                        חדר: %s
                        תאריך: %s
                        שעה: %s
                        מספר משתתפים: %d
                        ניסיון קודם: %s
                        טלפון: %s
                        אימייל: %s
                        הערות: %s

                        נשמח לראותך בפעם הבאה!
                        צוות Escape Center
                        """,
                    existing.getFirstName(), existing.getLastName(),
                    existing.getRoom(),
                    date,
                    timeRange,
                    existing.getParticipants(),
                    existing.getExperience(),
                    existing.getPhoneNumber(),
                    existing.getEmail(),
                    existing.getNotes().isEmpty() ? "אין" : existing.getNotes()
            );

            EmailService.sendEmail(
                    existing.getEmail(),
                    existing.getFirstName() + " " + existing.getLastName() + " - ביטול הזמנה Escape Center",
                    message
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
