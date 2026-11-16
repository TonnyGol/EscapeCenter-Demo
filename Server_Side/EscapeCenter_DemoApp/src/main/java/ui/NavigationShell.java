package ui;

import service.Session;
import javafx.animation.FadeTransition;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Single-window navigation shell with a persistent sidebar.
 * All screens render into the center content area instead of opening new windows.
 */
public class NavigationShell {

    private static final double NAV_ICON_SIZE = 18;
    private static final double NAV_ICON_GAP = 10;
    private static final double USER_ICON_SIZE = 14;
    private static final double BRAND_MARK_SIZE = 40;

    private final Stage stage;
    private final StackPane contentArea = new StackPane();
    private final VBox sidebar = new VBox();
    private Button activeButton;

    // Screen panes (lazy-initialized)
    private Node dashboardPane;
    private Node bookingPane;
    private Node clientsPane;

    // Booking screen reference for polling lifecycle
    private BookingManagementScreen bookingScreen;

    public NavigationShell(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        stage.setTitle("Escape Center — לוח בקרה");
        stage.setMinWidth(960);
        stage.setMinHeight(640);

        // ---- Sidebar ----
        sidebar.getStyleClass().add("sidebar");
        sidebar.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // App name
        Label appName = new Label("Escape Center");
        appName.getStyleClass().add("sidebar-app-name");

        // Username
        String username = Session.getInstance().getUsername();
        Label userLabel = Icons.label(username != null ? username : "", Icons.USER, USER_ICON_SIZE);
        userLabel.getStyleClass().add("sidebar-username");

        VBox header = new VBox(6, Icons.brandMark(BRAND_MARK_SIZE), appName, userLabel);
        header.getStyleClass().add("sidebar-header");

        // Separator
        Region sep = new Region();
        sep.getStyleClass().add("sidebar-separator");
        sep.setMaxWidth(Double.MAX_VALUE);

        // Navigation buttons
        Button dashBtn = createNavButton("לוח בקרה", Icons.DASHBOARD, "מעבר ללוח הבקרה הראשי");
        Button bookingBtn = createNavButton("הזמנות", Icons.CALENDAR, "ניהול הזמנות וחדרי בריחה");
        Button clientBtn = createNavButton("לקוחות", Icons.USERS, "ניהול מאגר לקוחות");
        Button reportsBtn = createNavButton("דו\"חות", Icons.REPORT, "משחקים ששוחקו והוזמנו השבוע והחודש");
        Button logoutBtn = createNavButton("התנתק", Icons.LOGOUT, "התנתקות וחזרה למסך הכניסה");
        logoutBtn.getStyleClass().add("nav-button-logout");

        // Spacer to push logout to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(
                header, sep,
                dashBtn, bookingBtn, clientBtn, reportsBtn,
                spacer, logoutBtn
        );

        // ---- Content Area ----
        contentArea.getStyleClass().add("content-area");
        contentArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // ---- Layout ----
        BorderPane root = new BorderPane();
        root.getStyleClass().add("nav-shell");
        root.setRight(sidebar);       // RTL: sidebar on right
        root.setCenter(contentArea);

        // ---- Button Actions ----
        dashBtn.setOnAction(e -> {
            setActiveButton(dashBtn);
            if (dashboardPane == null) dashboardPane = new DashboardScreen().createPane(this);
            switchContent(dashboardPane);
        });

        bookingBtn.setOnAction(e -> {
            setActiveButton(bookingBtn);
            // Stop previous polling if any
            if (bookingScreen != null) bookingScreen.stopPolling();
            // Always create fresh pane to get latest server data + start polling
            bookingScreen = new BookingManagementScreen();
            bookingPane = bookingScreen.createPane(stage);
            switchContent(bookingPane);
        });

        clientBtn.setOnAction(e -> {
            setActiveButton(clientBtn);
            if (clientsPane == null) clientsPane = new ClientsManagementScreen().createPane();
            switchContent(clientsPane);
        });

        reportsBtn.setOnAction(e -> {
            setActiveButton(reportsBtn);
            // A fresh pane each time, so the numbers are never from an earlier visit.
            switchContent(new ReportsScreen().createPane());
        });

        logoutBtn.setOnAction(e -> {
            // Stop polling before logout
            if (bookingScreen != null) bookingScreen.stopPolling();
            Session.getInstance().logout();
            stage.close();
            new LoginScreen().show(new Stage());
        });

        // Start on Dashboard
        dashBtn.fire();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                getClass().getClassLoader().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        // The login window is small and keeps its size when the scene changes, so the shell would stay cramped at the minimum.
        stage.setMaximized(true);
        stage.show();
        // Otherwise the first sidebar button keeps the focus ring while another screen is the active one.
        root.setFocusTraversable(true);
        root.requestFocus();
    }

    /** Navigate to booking screen (called from dashboard quick-actions). */
    public void navigateToBookings() {
        // Find the bookings button and fire it
        for (Node node : sidebar.getChildren()) {
            if (node instanceof Button btn && btn.getText().contains("הזמנות")) {
                btn.fire();
                break;
            }
        }
    }

    /** Navigate to clients screen. */
    public void navigateToClients() {
        for (Node node : sidebar.getChildren()) {
            if (node instanceof Button btn && btn.getText().contains("לקוחות")) {
                btn.fire();
                break;
            }
        }
    }

    /** Navigate to reports screen. */
    public void navigateToReports() {
        for (Node node : sidebar.getChildren()) {
            if (node instanceof Button btn && btn.getText().contains("דו\"חות")) {
                btn.fire();
                break;
            }
        }
    }

    // ---- Helpers ----

    private Button createNavButton(String text, String iconPath, String tooltipText) {
        Button btn = new Button(text);
        btn.setGraphic(Icons.of(iconPath, NAV_ICON_SIZE, btn.textFillProperty()));
        btn.setGraphicTextGap(NAV_ICON_GAP);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setAccessibleText(tooltipText);
        return btn;
    }

    private void setActiveButton(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        btn.getStyleClass().add("nav-button-active");
        activeButton = btn;
    }

    private void switchContent(Node newContent) {
        // Fade transition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), contentArea);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().setAll(newContent);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), contentArea);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }
}
