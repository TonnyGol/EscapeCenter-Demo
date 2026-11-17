package ui;

import service.ApiService;
import service.Session;
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

    private static final double FIELD_LABEL_GAP = 6;
    private static final double BRAND_MARK_SIZE = 56;
    private static final double WINDOW_WIDTH = 520;
    private static final double WINDOW_HEIGHT = 600;
    private static final double WINDOW_MIN_HEIGHT = 580; // the card is about 520 tall, with the title bar and some air around it

    public void show(Stage stage) {
        stage.setTitle("Escape Center — התחברות");
        stage.setMinWidth(500);
        stage.setMinHeight(WINDOW_MIN_HEIGHT);

        // ---- Card content ----
        Label title = new Label("Escape Center");
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
        VBox userRow = labeledField(userLabel, userField);
        VBox passRow = labeledField(passLabel, passField);

        VBox card = new VBox(14, Icons.brandMark(BRAND_MARK_SIZE), title, subtitle, userRow, passRow, loginBtn, statusLabel);
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

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(
                getClass().getClassLoader().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        // Auto-focus username field
        userField.requestFocus();
    }

    /** Label above its field, both reading right to left; the field fills the row so the text starts at the right edge. */
    private static VBox labeledField(Label label, TextField field) {
        field.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        VBox row = new VBox(FIELD_LABEL_GAP, label, field);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
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
