package com.example.escapecenter.ui;

import com.example.escapecenter.service.ApiService;
import com.example.escapecenter.service.Session;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Login screen — dark-themed centered card with keyboard navigation,
 * focus indicators, and animated error feedback.
 */
public class LoginScreen {

    public void show(Stage stage) {
        stage.setTitle("Escape Center — התחברות");
        stage.setMinWidth(500);
        stage.setMinHeight(400);

        // ---- Card content ----
        Label title = new Label("🔑 Escape Center");
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("התחבר למערכת ניהול חדרי הבריחה");
        subtitle.getStyleClass().add("login-subtitle");

        Label userLabel = new Label("שם משתמש:");
        userLabel.getStyleClass().add("login-label");

        TextField userField = new TextField();
        userField.setPromptText("הכנס שם משתמש");
        userField.getStyleClass().add("login-field");
        userField.setAccessibleText("שדה שם משתמש");
        userField.setTooltip(new Tooltip("הכנס את שם המשתמש שלך"));

        Label passLabel = new Label("סיסמה:");
        passLabel.getStyleClass().add("login-label");

        PasswordField passField = new PasswordField();
        passField.setPromptText("הכנס סיסמה");
        passField.getStyleClass().add("login-field");
        passField.setAccessibleText("שדה סיסמה");
        passField.setTooltip(new Tooltip("הכנס את הסיסמה שלך"));

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setAccessibleText("הודעת שגיאה");

        Button loginBtn = new Button("התחבר");
        loginBtn.getStyleClass().add("login-button");
        loginBtn.setAccessibleText("כפתור התחברות למערכת");
        loginBtn.setTooltip(new Tooltip("לחץ להתחברות"));
        loginBtn.setDefaultButton(true); // Enter key triggers this

        // ---- Layout ----
        HBox userRow = new HBox(10, userLabel, userField);
        userRow.setAlignment(Pos.CENTER_RIGHT);

        HBox passRow = new HBox(10, passLabel, passField);
        passRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(14, title, subtitle, userRow, passRow, loginBtn, statusLabel);
        card.getStyleClass().add("login-card");
        card.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        card.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-root");

        // ---- Login Action ----
        loginBtn.setOnAction(e -> {
            String username = userField.getText().trim();
            String password = passField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("אנא הכנס שם משתמש וסיסמה.");
                shakeNode(card);
                return;
            }

            // Disable while loading
            loginBtn.setDisable(true);
            loginBtn.setText("מתחבר...");

            // Run login in background so UI doesn't freeze
            new Thread(() -> {
                boolean success = ApiService.getInstance().login(username, password);
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        NavigationShell shell = new NavigationShell(stage);
                        shell.show();
                    } else {
                        statusLabel.setText("שם משתמש או סיסמה לא נכונים.");
                        loginBtn.setDisable(false);
                        loginBtn.setText("התחבר");
                        shakeNode(card);
                    }
                });
            }).start();
        });

        // ---- Keyboard: Escape to clear ----
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                userField.clear();
                passField.clear();
                statusLabel.setText("");
                userField.requestFocus();
            }
        });

        Scene scene = new Scene(root, 520, 440);
        scene.getStylesheets().add(
                getClass().getClassLoader().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        // Auto-focus username field
        userField.requestFocus();
    }

    /** Shake animation for error feedback. */
    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(80), node);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.setOnFinished(e -> node.setTranslateX(0));
        shake.play();
    }
}
