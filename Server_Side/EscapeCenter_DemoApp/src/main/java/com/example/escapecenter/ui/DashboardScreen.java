package com.example.escapecenter.ui;

import com.example.escapecenter.service.Session;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Dashboard screen — quick-action card grid.
 * Returns a pane for embedding inside NavigationShell.
 */
public class DashboardScreen {

    public Node createPane(NavigationShell shell) {
        String username = Session.getInstance().getUsername();

        // Welcome header
        Label welcome = new Label("👋 שלום, " + (username != null ? username : ""));
        welcome.getStyleClass().add("dashboard-welcome");

        Label sub = new Label("מה תרצה לעשות היום?");
        sub.getStyleClass().add("dashboard-sub");

        // Quick-action cards
        VBox bookingCard = createCard("📅", "ניהול הזמנות",
                "צפייה ועריכת הזמנות בלוח השבועי");
        VBox clientCard = createCard("👥", "ניהול לקוחות",
                "חיפוש וצפייה בפרטי לקוחות");
        VBox reportsCard = createCard("📋", "דו\"חות",
                "צפייה בנתונים סטטיסטיים — בקרוב");
        VBox settingsCard = createCard("⚙️", "הגדרות",
                "הגדרות מערכת — בקרוב");

        // Card actions
        bookingCard.setOnMouseClicked(e -> shell.navigateToBookings());
        clientCard.setOnMouseClicked(e -> shell.navigateToClients());
        reportsCard.setOnMouseClicked(e -> showInfo("דו\"חות — בקרוב"));
        settingsCard.setOnMouseClicked(e -> showInfo("הגדרות — בקרוב"));

        // Grid layout (2×2)
        GridPane grid = new GridPane();
        grid.getStyleClass().add("dashboard-grid");
        grid.setAlignment(Pos.CENTER);
        grid.add(bookingCard, 0, 0);
        grid.add(clientCard, 1, 0);
        grid.add(reportsCard, 0, 1);
        grid.add(settingsCard, 1, 1);

        VBox pane = new VBox(20, welcome, sub, grid);
        pane.getStyleClass().add("dashboard-pane");
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        pane.setAlignment(Pos.TOP_RIGHT);
        return pane;
    }

    /** Legacy method — still supports opening in separate stage if needed. */
    public void show(Stage stage) {
        // Redirect to NavigationShell
        NavigationShell shell = new NavigationShell(stage);
        shell.show();
    }

    private VBox createCard(String icon, String title, String description) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("dashboard-card-icon");

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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
