package ui;

import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Line icons drawn as paths on a 24 by 24 grid, one stroke weight everywhere. They replace emoji, which look
 * different on every machine and cannot take the theme colors. Pass a button's or label's text fill to have the
 * icon follow hover and selected colors.
 */
final class Icons {

    static final String DASHBOARD = "M3 3h8v8H3z M13 3h8v5h-8z M13 12h8v9h-8z M3 15h8v6H3z";
    static final String CALENDAR = "M4 5h16v16H4z M4 10h16 M8 3v4 M16 3v4";
    static final String USERS = "M9 4a3.5 3.5 0 1 1 0 7 3.5 3.5 0 1 1 0-7z M2 21c0-3.6 3-6 7-6s7 2.4 7 6 M16.5 4.2a3.3 3.3 0 0 1 0 6.3 M18 15.3c2.4.6 4 2.5 4 5.7";
    static final String USER = "M12 3.5a4 4 0 1 1 0 8 4 4 0 1 1 0-8z M4 21c0-4 3.6-6 8-6s8 2 8 6";
    static final String REPORT = "M4 3v18h17 M9 17v-6 M14 17V7 M19 17v-4";
    static final String LOGOUT = "M9 21H4V3h5 M16 16l4-4-4-4 M20 12H9";
    static final String PHONE = "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z";
    static final String MAIL = "M3 5h18v14H3z M3 7l9 6 9-6";
    static final String KEYHOLE = "M12 3a5 5 0 0 1 2.5 9.3L16 21H8l1.5-8.7A5 5 0 0 1 12 3z";

    private static final double GRID_SIZE = 24;
    private static final double STROKE_WIDTH = 2;
    private static final double LABEL_ICON_GAP = 8;
    private static final double BRAND_MARK_ICON_RATIO = 0.6; // keyhole size inside the rounded square

    private Icons() {
    }

    /** An icon of {@code size} pixels whose lines follow {@code stroke}. */
    static Node of(String pathData, double size, ObservableValue<? extends Paint> stroke) {
        SVGPath path = shape(pathData, size);
        path.strokeProperty().bind(stroke);
        return new Group(path);
    }

    static Node of(String pathData, double size, Color stroke) {
        SVGPath path = shape(pathData, size);
        path.setStroke(stroke);
        return new Group(path);
    }

    /** The venue's mark: a white keyhole on an accent rounded square, the same keyhole as the website's favicon. */
    static Node brandMark(double size) {
        StackPane mark = new StackPane(of(KEYHOLE, size * BRAND_MARK_ICON_RATIO, Color.WHITE));
        mark.getStyleClass().add("brand-mark");
        mark.setMinSize(size, size);
        mark.setPrefSize(size, size);
        mark.setMaxSize(size, size);
        return mark;
    }

    /** A label with the icon before the text, in the label's own text color. */
    static Label label(String text, String pathData, double size) {
        Label label = new Label(text);
        label.setGraphic(of(pathData, size, label.textFillProperty()));
        label.setGraphicTextGap(LABEL_ICON_GAP);
        return label;
    }

    private static SVGPath shape(String pathData, double size) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setFill(null);
        path.setStrokeWidth(STROKE_WIDTH);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.setScaleX(size / GRID_SIZE);
        path.setScaleY(size / GRID_SIZE);
        return path;
    }
}
