package ui;

import model.Booking;
import model.Room;
import javafx.beans.binding.Bindings;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * One day with time running down the page and one column per room. Every game sits at its real start and end,
 * so games in different rooms that run at the same time line up, and the minutes they share get a shaded band
 * across all rooms.
 */
final class DayTimelineView {

    /** Creates the button of one game slot, booked or free. */
    @FunctionalInterface
    interface SlotButtonFactory {
        Button create(LocalDate date, String roomName, LocalTime start);
    }

    /** Minutes during which games of two different rooms run together. */
    record Span(LocalDateTime start, LocalDateTime end) {
    }

    private static final double PIXELS_PER_MINUTE = 1.3;
    private static final double AXIS_WIDTH = 44;
    private static final double GUTTER_WIDTH = 84;
    private static final double COLUMN_MIN_WIDTH = 84; // 7 rooms and the overlap column fit a 1280px window; narrower windows scroll
    private static final double TOP_PADDING = 12;
    private static final double BLOCK_SIDE_INSET = 3;
    private static final double BLOCK_VERTICAL_GAP = 2;
    private static final double NOW_LINE_HEIGHT = 2;
    private static final int MINUTES_PER_HOUR = 60;
    private static final String EMPTY_MESSAGE = "אין משחקים מוגדרים לחדרים.";
    private static final String OVERLAP_LABEL = "חפיפה";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private DayTimelineView() {
    }

    static Node build(List<Room> rooms, Map<String, List<LocalTime>> gameStarts, Collection<Booking> dayBookings,
                      LocalDate date, SlotButtonFactory slotFactory, ScrollPane scrollPane) {
        int firstMinute = Integer.MAX_VALUE;
        int lastMinute = 0;
        for (Room room : rooms) {
            for (LocalTime start : gameStarts.getOrDefault(room.name(), List.of())) {
                firstMinute = Math.min(firstMinute, minuteOfDay(start));
                lastMinute = Math.max(lastMinute, minuteOfDay(start) + room.durationMinutes());
            }
        }
        if (firstMinute == Integer.MAX_VALUE) {
            return new Label(EMPTY_MESSAGE);
        }
        int timelineStart = firstMinute / MINUTES_PER_HOUR * MINUTES_PER_HOUR;
        int timelineEnd = Math.ceilDiv(lastMinute, MINUTES_PER_HOUR) * MINUTES_PER_HOUR;
        double bodyHeight = (timelineEnd - timelineStart) * PIXELS_PER_MINUTE + 2 * TOP_PADDING;

        Pane gridLayer = hourLines(timelineStart, timelineEnd);
        HBox columnsRow = columnsRow(rooms, gameStarts, date, slotFactory, timelineStart, timelineEnd, bodyHeight);
        Pane bandLayer = overlapBands(dayBookings, timelineStart);
        Pane overlayLayer = overlayLayer(dayBookings, date, timelineStart, timelineEnd);

        // The body is laid out left to right so the shaded bands can start at x = 0; only the row of columns reads right to left.
        StackPane body = new StackPane(gridLayer, bandLayer, columnsRow, overlayLayer);
        body.setAlignment(Pos.TOP_LEFT);
        body.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        body.setMinHeight(bodyHeight);
        body.setPrefHeight(bodyHeight);

        HBox header = roomHeaders(rooms);
        VBox content = new VBox(header, body);
        stickToTop(header, content, scrollPane);
        return content;
    }

    /** Minutes of the day when two games of different rooms run together, joined into continuous spans. */
    static List<Span> overlapSpans(Collection<Booking> bookings) {
        List<Span> joined = new ArrayList<>();
        for (OverlapPanel.Entry entry : OverlapPanel.entries(bookings, null)) {
            Span last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
            if (last != null && !entry.start().isAfter(last.end())) {
                joined.set(joined.size() - 1, new Span(last.start(), max(last.end(), entry.end())));
            } else {
                joined.add(new Span(entry.start(), entry.end()));
            }
        }
        return joined;
    }

    private static HBox columnsRow(List<Room> rooms, Map<String, List<LocalTime>> gameStarts, LocalDate date,
                                   SlotButtonFactory slotFactory, int timelineStart, int timelineEnd, double bodyHeight) {
        HBox row = new HBox();
        row.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        row.getChildren().add(hourLabels(timelineStart, timelineEnd, bodyHeight));
        for (Room room : rooms) {
            Pane column = new Pane();
            column.getStyleClass().add("timeline-column");
            column.setMinWidth(COLUMN_MIN_WIDTH);
            column.setPrefSize(COLUMN_MIN_WIDTH, bodyHeight);
            HBox.setHgrow(column, Priority.ALWAYS);
            for (LocalTime start : gameStarts.getOrDefault(room.name(), List.of())) {
                column.getChildren().add(gameBlock(column, slotFactory.create(date, room.name(), start),
                        minuteOfDay(start), room.durationMinutes(), timelineStart));
            }
            row.getChildren().add(column);
        }
        row.getChildren().add(spacer(GUTTER_WIDTH));
        return row;
    }

    private static Button gameBlock(Pane column, Button block, int startMinute, int durationMinutes, int timelineStart) {
        block.getStyleClass().add("timeline-block");
        block.setMinSize(0, 0);
        block.setLayoutX(BLOCK_SIDE_INSET);
        block.setLayoutY(yOf(startMinute, timelineStart) + BLOCK_VERTICAL_GAP);
        block.setPrefHeight(durationMinutes * PIXELS_PER_MINUTE - 2 * BLOCK_VERTICAL_GAP);
        block.prefWidthProperty().bind(column.widthProperty().subtract(2 * BLOCK_SIDE_INSET));
        return block;
    }

    private static Pane hourLabels(int timelineStart, int timelineEnd, double bodyHeight) {
        Pane axis = new Pane();
        axis.setMinWidth(AXIS_WIDTH);
        axis.setPrefSize(AXIS_WIDTH, bodyHeight);
        for (int minute = timelineStart; minute <= timelineEnd; minute += MINUTES_PER_HOUR) {
            Label hour = new Label(LocalTime.of(minute / MINUTES_PER_HOUR % 24, 0).format(TIME_FORMAT));
            hour.getStyleClass().add("timeline-hour-label");
            hour.setPrefWidth(AXIS_WIDTH);
            hour.setLayoutY(yOf(minute, timelineStart) - 9);
            axis.getChildren().add(hour);
        }
        return axis;
    }

    private static Pane hourLines(int timelineStart, int timelineEnd) {
        Pane lines = new Pane();
        for (int minute = timelineStart; minute <= timelineEnd; minute += MINUTES_PER_HOUR) {
            Region line = new Region();
            line.getStyleClass().add("timeline-hour-line");
            line.setLayoutY(yOf(minute, timelineStart));
            line.prefWidthProperty().bind(lines.widthProperty().subtract(AXIS_WIDTH));
            lines.getChildren().add(line);
        }
        return lines;
    }

    /** Shaded bands where games overlap. They sit behind the games, so a booking is never covered by its own band. */
    private static Pane overlapBands(Collection<Booking> dayBookings, int timelineStart) {
        Pane bands = new Pane();
        bands.setMouseTransparent(true);
        for (Span span : overlapSpans(dayBookings)) {
            Region band = new Region();
            band.getStyleClass().add("overlap-band");
            band.setLayoutY(yOf(minuteOfDay(span.start().toLocalTime()), timelineStart));
            band.setPrefHeight(java.time.Duration.between(span.start(), span.end()).toMinutes() * PIXELS_PER_MINUTE);
            band.prefWidthProperty().bind(bands.widthProperty().subtract(AXIS_WIDTH));
            bands.getChildren().add(band);
        }
        return bands;
    }

    /** The label of each overlap and the line for the current time, above the games. Clicks pass through. */
    private static Pane overlayLayer(Collection<Booking> dayBookings, LocalDate date, int timelineStart, int timelineEnd) {
        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);
        for (Span span : overlapSpans(dayBookings)) {
            double top = yOf(minuteOfDay(span.start().toLocalTime()), timelineStart);
            Label pill = new Label(OVERLAP_LABEL + "\n" + span.start().format(TIME_FORMAT) + "-" + span.end().format(TIME_FORMAT));
            pill.getStyleClass().add("overlap-pill");
            pill.setLayoutX(4);
            pill.setLayoutY(top + 2);
            pill.setPrefWidth(GUTTER_WIDTH - 8);
            overlay.getChildren().add(pill);
        }
        int nowMinute = minuteOfDay(LocalTime.now());
        if (date.equals(LocalDate.now()) && nowMinute >= timelineStart && nowMinute <= timelineEnd) {
            Region nowLine = new Region();
            nowLine.getStyleClass().add("now-line");
            nowLine.setLayoutY(yOf(nowMinute, timelineStart) - NOW_LINE_HEIGHT / 2);
            nowLine.setPrefHeight(NOW_LINE_HEIGHT);
            nowLine.prefWidthProperty().bind(overlay.widthProperty().subtract(AXIS_WIDTH));
            overlay.getChildren().add(nowLine);
        }
        return overlay;
    }

    private static HBox roomHeaders(List<Room> rooms) {
        HBox header = new HBox();
        header.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        header.getStyleClass().add("timeline-header");
        header.getChildren().add(spacer(AXIS_WIDTH));
        for (Room room : rooms) {
            Label name = new Label(room.name());
            name.getStyleClass().add("timeline-room-header");
            name.setMinWidth(COLUMN_MIN_WIDTH);
            name.setPrefWidth(COLUMN_MIN_WIDTH);
            name.setMaxWidth(Double.MAX_VALUE);
            name.setWrapText(true);
            HBox.setHgrow(name, Priority.ALWAYS);
            header.getChildren().add(name);
        }
        header.getChildren().add(spacer(GUTTER_WIDTH));
        return header;
    }

    /** Keeps the room names in view while the day scrolls. */
    private static void stickToTop(Node header, Region content, ScrollPane scrollPane) {
        header.setViewOrder(-1);
        header.translateYProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0, content.getHeight() - scrollPane.getViewportBounds().getHeight()) * scrollPane.getVvalue(),
                scrollPane.vvalueProperty(), scrollPane.viewportBoundsProperty(), content.heightProperty()));
    }

    private static Region spacer(double width) {
        Region spacer = new Region();
        spacer.setMinWidth(width);
        spacer.setPrefWidth(width);
        spacer.setMaxWidth(width);
        return spacer;
    }

    private static double yOf(int minuteOfDay, int timelineStart) {
        return (minuteOfDay - timelineStart) * PIXELS_PER_MINUTE + TOP_PADDING;
    }

    private static int minuteOfDay(LocalTime time) {
        return time.getHour() * MINUTES_PER_HOUR + time.getMinute();
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
