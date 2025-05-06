package com.example.EscapeCenter_Demo.gui;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DashboardScreen {
    public void start(Stage stage, String username) {
        stage.setTitle("Escape Center - לוח בקרה");

        Label welcomeLabel = new Label("ברוך הבא, " + username);
        welcomeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button clientBtn = new Button("ניהול לקוחות");
        Button bookingBtn = new Button("ניהול הזמנות");
        Button reportsBtn = new Button("דו\"חות");
        Button logoutBtn = new Button("התנתק");

        clientBtn.setPrefWidth(200);
        bookingBtn.setPrefWidth(200);
        reportsBtn.setPrefWidth(200);
        logoutBtn.setPrefWidth(200);

        VBox vbox = new VBox(15, welcomeLabel, clientBtn, bookingBtn, reportsBtn, logoutBtn);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(30));
        vbox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT); // RTL for Hebrew

        clientBtn.setOnAction(e -> showAlert("ניהול לקוחות - טרם מומש"));
        bookingBtn.setOnAction(e -> new BookingManagementScreen().start(new Stage()));
        reportsBtn.setOnAction(e -> showAlert("דו\"חות - טרם מומש"));
        logoutBtn.setOnAction(e -> {
            stage.close();
            new LoginScreen().start(new Stage());
        });

        Scene scene = new Scene(vbox, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
