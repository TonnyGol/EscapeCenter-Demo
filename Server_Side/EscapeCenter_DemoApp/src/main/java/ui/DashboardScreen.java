package ui;

import service.Session;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Dashboard screen — quick-action card grid.
 * Returns a pane for embedding inside NavigationShell.
 */
public class DashboardScreen {

    private static final double CARD_ICON_SIZE = 36;
    private static final String CARD_ICON_COLOR = "#c8452d";

    public Node createPane(NavigationShell shell) {
        String username = Session.getInstance().getUsername();

        // Welcome header
        Label welcome = new Label("שלום, " + (username != null ? username : ""));
        welcome.getStyleClass().add("dashboard-welcome");

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

        VBox pane = new VBox(20, welcome, sub, grid);
        pane.getStyleClass().add("dashboard-pane");
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        pane.setAlignment(Pos.TOP_LEFT);
        return pane;
    }

    /** Legacy method — still supports opening in separate stage if needed. */
    public void show(Stage stage) {
        // Redirect to NavigationShell
        NavigationShell shell = new NavigationShell(stage);
        shell.show();
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
