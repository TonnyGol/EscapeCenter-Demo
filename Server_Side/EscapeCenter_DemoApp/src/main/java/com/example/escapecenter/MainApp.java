package com.example.escapecenter;

import com.example.escapecenter.ui.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the EscapeCenter desktop application.
 * Launches the JavaFX login screen with dark theme.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Set minimum window size
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(400);

        new LoginScreen().show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
