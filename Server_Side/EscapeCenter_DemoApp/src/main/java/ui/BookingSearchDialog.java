package ui;

import model.Booking;
import model.BookingStatus;
import service.ApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Finds bookings by client name, phone or email and by status; the chosen booking is shown on the calendar. */
final class BookingSearchDialog {

    private static final long SEARCH_DAYS_EACH_WAY = 180;
    private static final double DIALOG_WIDTH = 720;
    private static final double DIALOG_HEIGHT = 420;
    private static final String TITLE = "חיפוש הזמנות";
    private static final String QUERY_PROMPT = "שם, טלפון או אימייל";
    private static final String ANY_STATUS_LABEL = "כל הסטטוסים";
    private static final String SEARCH_LABEL = "חפש";
    private static final String SHOW_ON_CALENDAR_LABEL = "הצג בלוח";
    private static final String CLOSE_LABEL = "סגור";
    private static final String NO_RESULTS_LABEL = "לא נמצאו הזמנות.";
    private static final String SEARCH_FAILED_TITLE = "החיפוש נכשל";
    private static final String SEARCH_FAILED_MESSAGE = "לא ניתן להתחבר לשרת.";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private BookingSearchDialog() {
    }

    static Optional<Booking> pick(Stage owner) {
        TextField queryField = new TextField();
        queryField.setPromptText(QUERY_PROMPT);
        queryField.setAccessibleText(QUERY_PROMPT);
        HBox.setHgrow(queryField, Priority.ALWAYS);

        // A null item stands for "any status".
        ComboBox<BookingStatus> statusBox = new ComboBox<>();
        statusBox.getItems().add(null);
        statusBox.getItems().addAll(BookingStatus.values());
        statusBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BookingStatus status) {
                return status == null ? ANY_STATUS_LABEL : status.toString();
            }

            @Override
            public BookingStatus fromString(String label) {
                return null;
            }
        });
        statusBox.setValue(null);
        statusBox.setAccessibleText(ANY_STATUS_LABEL);

        Button searchButton = new Button(SEARCH_LABEL);
        searchButton.setDefaultButton(true);

        TableView<Booking> table = new TableView<>();
        table.setPlaceholder(new Label(NO_RESULTS_LABEL));
        table.getColumns().add(column("תאריך", booking -> booking.start().format(DATE_TIME_FORMAT)));
        table.getColumns().add(column("חדר", Booking::roomName));
        table.getColumns().add(column("לקוח", booking -> booking.firstName() + " " + booking.lastName()));
        table.getColumns().add(column("טלפון", Booking::phoneNumber));
        table.getColumns().add(column("סטטוס", booking -> statusOf(booking).toString()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // The request runs off the screen thread, so a slow or stopped server does not freeze the window.
        Runnable search = () -> {
            LocalDate today = LocalDate.now();
            String text = queryField.getText();
            BookingStatus status = statusBox.getValue();
            searchButton.setDisable(true);
            Thread request = new Thread(() -> {
                Optional<List<Booking>> found = ApiService.getInstance().searchBookings(
                        today.minusDays(SEARCH_DAYS_EACH_WAY), today.plusDays(SEARCH_DAYS_EACH_WAY), text, status);
                Platform.runLater(() -> {
                    searchButton.setDisable(false);
                    if (found.isPresent()) {
                        table.getItems().setAll(found.get());
                        return;
                    }
                    Alert alert = new Alert(Alert.AlertType.ERROR, SEARCH_FAILED_MESSAGE);
                    alert.setTitle(SEARCH_FAILED_TITLE);
                    alert.setHeaderText(null);
                    alert.initOwner(owner);
                    alert.show();
                });
            });
            request.setDaemon(true);
            request.start();
        };
        searchButton.setOnAction(e -> search.run());

        VBox content = new VBox(10, new HBox(8, queryField, statusBox, searchButton), table);
        content.setPrefSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        content.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(TITLE);
        dialog.getDialogPane().setContent(content);
        ButtonType show = new ButtonType(SHOW_ON_CALENDAR_LABEL, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(show, new ButtonType(CLOSE_LABEL, ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().lookupButton(show).disableProperty()
                .bind(table.getSelectionModel().selectedItemProperty().isNull());

        search.run();
        return dialog.showAndWait()
                .filter(show::equals)
                .map(button -> table.getSelectionModel().getSelectedItem());
    }

    /** Old server answers have no status; those bookings are confirmed. */
    static BookingStatus statusOf(Booking booking) {
        return booking.status() != null ? booking.status() : BookingStatus.CONFIRMED;
    }

    private static TableColumn<Booking, String> column(String title, Function<Booking, String> text) {
        TableColumn<Booking, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(text.apply(cell.getValue())));
        return column;
    }
}
