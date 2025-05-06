package com.example.EscapeCenter_Demo.gui;

import javafx.application.Application;
import javafx.stage.Stage;

public class ServerGUI extends Application {
    @Override
    public void start(Stage primaryStage) {
        new LoginScreen().start(primaryStage);
    }

    public static void launchApp() {
        launch();
    }
}
