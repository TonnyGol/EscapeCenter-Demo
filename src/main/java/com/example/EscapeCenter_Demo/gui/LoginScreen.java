package com.example.EscapeCenter_Demo.gui;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

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

            loginRequest(username, password, stage);
        });


        Scene scene = new Scene(vbox, 350, 200);
        stage.setScene(scene);
        stage.show();
    }
    
    private void loginRequest(String username, String password, Stage stage) {
        JEditorPane statusLabel = new JEditorPane();
        try {
            // Encode "username:password" to Base64
            String authString = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/server/test"))  // protected endpoint
                    .header("Authorization", "Basic " + encodedAuth)
                    .GET()
                    .build();

            // Send request
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                new DashboardScreen().start(new Stage(), username, client, encodedAuth);
                stage.close();
            } else {
                statusLabel.setText("שם משתמש או סיסמה לא נכונים. נסה שוב.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("שגיאה בהתחברות לשרת.");
        }
    }
}
