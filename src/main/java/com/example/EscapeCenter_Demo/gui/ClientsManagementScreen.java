package com.example.EscapeCenter_Demo.gui;

import com.example.EscapeCenter_Demo.Client;
import com.example.EscapeCenter_Demo.Booking;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class ClientsManagementScreen {

    private List<Client> clients;
    private Map<String, Client> clientMap = new HashMap<>();
    private Map<String, Booking> bookings = new HashMap<>();
    private VBox cardDisplay;
    private Button pageBtn;
    private TextField searchField;

    private int clientsPerPage = 10;
    private int currentPage = 0;

    public void start(Stage stage, HttpClient httpClient, String encodedAuth) {
        stage.setTitle("ניהול לקוחות");

        loadAllBookings(httpClient, bookings);
        loadAllClients(httpClient, clientMap, encodedAuth);
        clients = new ArrayList<>(clientMap.values());

        BorderPane mainLayout = new BorderPane();

        // Top: Search bar
        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(15));
        searchBar.setAlignment(Pos.CENTER);
        searchField = new TextField();
        searchField.setPromptText("חפש לקוח לפי שם או טלפון...");
        Button searchButton = new Button("🔍");
        searchButton.setOnAction(e -> filterClients());
        searchBar.getChildren().addAll(searchField, searchButton);
        mainLayout.setTop(searchBar);

        // Center: Client cards display
        cardDisplay = new VBox(15);
        cardDisplay.setPadding(new Insets(20));
        cardDisplay.setAlignment(Pos.TOP_CENTER);
        mainLayout.setCenter(cardDisplay);

        // Bottom: Navigation with page counter
        HBox navigation = new HBox(10);
        navigation.setPadding(new Insets(10));
        navigation.setAlignment(Pos.CENTER);

        Button leftBtn = new Button("⬅");
        Button rightBtn = new Button("➡");
        pageBtn = new Button();

        leftBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                showClientsPage();
            }
        });

        rightBtn.setOnAction(e -> {
            if ((currentPage + 1) * clientsPerPage < clients.size()) {
                currentPage++;
                showClientsPage();
            }
        });

        pageBtn.setOnAction(e -> {
            int totalPages = (int) Math.ceil((double) clients.size() / clientsPerPage);
            TextInputDialog dialog = new TextInputDialog(String.valueOf(currentPage + 1));
            dialog.setTitle("מעבר לעמוד");
            dialog.setHeaderText("הזן מספר עמוד (1 עד " + totalPages + ")");
            dialog.setContentText("עמוד:");

            dialog.showAndWait().ifPresent(input -> {
                try {
                    int page = Integer.parseInt(input) - 1;
                    if (page >= 0 && page < totalPages) {
                        currentPage = page;
                        showClientsPage();
                    }
                } catch (NumberFormatException ignored) {
                }
            });
        });


        navigation.getChildren().addAll(leftBtn, pageBtn, rightBtn);
        mainLayout.setBottom(navigation);

        showClientsPage();

        stage.setScene(new Scene(mainLayout, 600, 600));
        stage.show();
    }

    private void filterClients() {
        String keyword = searchField.getText().toLowerCase();

        clients = clientMap.values().stream()
                .filter(client ->
                        (client.getFirstName() + " " + client.getLastName()).toLowerCase().contains(keyword) ||
                                client.getPhoneNumber().toLowerCase().contains(keyword)
                )
                .collect(Collectors.toList());

        currentPage = 0;
        showClientsPage();
    }

    private void showClientsPage() {
        cardDisplay.getChildren().clear();

        if (clients.isEmpty()) {
            cardDisplay.getChildren().add(new Label("לא נמצאו לקוחות."));
            return;
        }

        int start = currentPage * clientsPerPage;
        int end = Math.min(start + clientsPerPage, clients.size());
        List<Client> pageClients = clients.subList(start, end);

        for (Client client : pageClients) {
            cardDisplay.getChildren().add(createClientCard(client));
        }

        // Update page button
        int totalPages = (int) Math.ceil((double) clients.size() / clientsPerPage);
        pageBtn.setText("עמוד " + (currentPage + 1) + " / " + totalPages);
    }

    private VBox createClientCard(Client client) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f0f9ff; -fx-border-color: #cbd5e1; -fx-border-radius: 10; -fx-background-radius: 10;");
        card.setAlignment(Pos.CENTER_RIGHT);
        card.setPrefWidth(500);

        String fullName = client.getFirstName() + " " + client.getLastName();
        Label name = new Label("👤 " + fullName);
        name.setFont(new Font(16));

        Label phone = new Label("📞 " + client.getPhoneNumber());
        Label email = new Label("✉️ " + client.getEmail());

        card.getChildren().addAll(name, phone, email);

        Booking booking = bookings.get(fullName);
        if (booking != null) {
            Label experience = new Label("🎯 ניסיון: " + booking.getExperience());
            card.getChildren().addAll(name, phone, email, experience);
        }

        return card;
    }

    private void loadAllBookings(HttpClient client, Map<String, Booking> bookings){
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/bookings"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();

                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();

                Map<String, Booking> serverBookings =
                        mapper.readValue(json, new TypeReference<>() {});


                bookings.clear();
                bookings.putAll(serverBookings);

            } else {
                System.out.println("Server returned status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAllClients(HttpClient client, Map<String,
            Client> clientMap, String encodedAuth){
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/server/clients"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());


            if (response.statusCode() == 200) {
                String json = response.body();

                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();

                Map<String, Client> serverBookings =
                        mapper.readValue(json, new TypeReference<>() {});


                clientMap.clear();
                clientMap.putAll(serverBookings);

                System.out.println("Loaded " + clientMap.size() + " clients from server.");

            } else {
                System.out.println("Server returned status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
