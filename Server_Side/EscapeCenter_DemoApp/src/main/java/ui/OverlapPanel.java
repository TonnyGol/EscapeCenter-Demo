package ui;

import model.Booking;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Side panel next to the booking calendar that lists every overlap of the week, one message each. */
final class OverlapPanel {

    /** Two games in different rooms that run together from {@code start} to {@code end}. */
    record Entry(LocalDate date, LocalDateTime start, LocalDateTime end, String firstRoom, String secondRoom) {
    }

    private static final double WIDTH = 240;
    private static final String TITLE = "חפיפות השבוע";
    private static final String NONE_MESSAGE = "אין חפיפות השבוע.";
    private static final String PICK_HINT = "לחצו כדי לעבור ליום הזה";
    private static final String ROOMS_SEPARATOR = " ↔ ";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final VBox root = new VBox(10);

    OverlapPanel() {
        root.getStyleClass().add("overlap-panel");
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setMinWidth(WIDTH);
        root.setPrefWidth(WIDTH);
        root.setMaxWidth(WIDTH);
    }

    Node node() {
        return root;
    }

    /** One entry for each pair of overlapping games, earliest first. With {@code onlyRoom}, only pairs that include that room. */
    static List<Entry> entries(Collection<Booking> bookings, String onlyRoom) {
        List<Booking> list = new ArrayList<>(bookings);
        List<Entry> found = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Booking a = list.get(i);
                Booking b = list.get(j);
                if (a.roomName().equals(b.roomName()) || !a.overlaps(b)) {
                    continue;
                }
                boolean putSecondFirst = onlyRoom != null && onlyRoom.equals(b.roomName());
                if (onlyRoom != null && !onlyRoom.equals(a.roomName()) && !putSecondFirst) {
                    continue;
                }
                LocalDateTime start = a.start().isAfter(b.start()) ? a.start() : b.start();
                LocalDateTime end = a.end().isBefore(b.end()) ? a.end() : b.end();
                found.add(putSecondFirst
                        ? new Entry(start.toLocalDate(), start, end, b.roomName(), a.roomName())
                        : new Entry(start.toLocalDate(), start, end, a.roomName(), b.roomName()));
            }
        }
        found.sort(Comparator.comparing(Entry::start).thenComparing(Entry::firstRoom).thenComparing(Entry::secondRoom));
        return found;
    }

    void show(List<Entry> entries, Function<LocalDate, String> dayLabel, Consumer<LocalDate> onPick) {
        Label title = new Label(TITLE + " (" + entries.size() + ")");
        title.getStyleClass().add("overlap-panel-title");

        VBox items = new VBox(8);
        if (entries.isEmpty()) {
            Label none = new Label(NONE_MESSAGE);
            none.getStyleClass().add("overlap-none");
            items.getChildren().add(none);
        }
        for (Entry entry : entries) {
            Button item = new Button(dayLabel.apply(entry.date()) + "\n" + entry.start().format(TIME_FORMAT) + "-"
                    + entry.end().format(TIME_FORMAT) + "\n" + entry.firstRoom() + ROOMS_SEPARATOR + entry.secondRoom());
            item.getStyleClass().add("overlap-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setTooltip(new Tooltip(PICK_HINT));
            item.setOnAction(e -> onPick.accept(entry.date()));
            items.getChildren().add(item);
        }

        ScrollPane scroll = new ScrollPane(items);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("overlap-scroll");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().setAll(title, scroll);
    }
}
