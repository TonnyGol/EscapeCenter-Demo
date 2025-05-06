package com.example.EscapeCenter_Demo.gui;

import com.example.EscapeCenter_Demo.WorkersService;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginScreen {
    public void start(Stage stage) {
        stage.setTitle("Escape Center - התחברות עובד");

        Label userLabel = new Label("שם משתמש:");
        TextField userField = new TextField();
        userField.setPromptText("הכנס שם משתמש");

        Label passLabel = new Label("סיסמה:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("הכנס סיסמה");

        Label statusLabel = new Label();

        Button loginBtn = new Button("התחבר");
        loginBtn.setPrefWidth(100);

        userLabel.setMinWidth(80);
        passLabel.setMinWidth(80);

        VBox vbox = new VBox(10,
                new HBox(10, userLabel, userField),
                new HBox(10, passLabel, passField),
                loginBtn,
                statusLabel
        );
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT); // Make everything RTL

        loginBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();
            if (WorkersService.authenticate(username, password)) {
                new DashboardScreen().start(new Stage(), username);
                stage.close();
            } else {
                statusLabel.setText("שם משתמש או סיסמה לא נכונים. נסה שוב.");
            }
        });

        Scene scene = new Scene(vbox, 350, 200);
        stage.setScene(scene);
        stage.show();
    }
}
