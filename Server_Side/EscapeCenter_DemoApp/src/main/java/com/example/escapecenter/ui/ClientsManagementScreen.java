package com.example.escapecenter.ui;

import com.example.escapecenter.model.Booking;
import com.example.escapecenter.model.Client;
import com.example.escapecenter.service.ApiService;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clients management screen — real-time searchable, paginated client list
 * with dark-themed cards. Returns a pane for embedding in NavigationShell.
 */
public class ClientsManagementScreen {

    private List<Client> clients;
    private Map<String, Client> clientMap = new HashMap<>();
    private Map<String, Booking> bookings = new HashMap<>();
    private VBox cardDisplay;
    private Label pageLabel;
    private Label countLabel;
    private TextField searchField;
    private Button leftBtn;
    private Button rightBtn;

    private static final int CLIENTS_PER_PAGE = 10;
    private int currentPage = 0;

    /**
     * Creates the clients pane for embedding inside NavigationShell.
     */
    public Node createPane() {
        // Load data from server
        bookings = new HashMap<>(ApiService.getInstance().loadBookings());
        clientMap = new HashMap<>(ApiService.getInstance().loadClients());
        clients = new ArrayList<>(clientMap.values());

        // ---- Search bar ----
        searchField = new TextField();
        searchField.setPromptText("🔍 חפש לקוח לפי שם או טלפון...");
        searchField.getStyleClass().add("search-field");
        searchField.setAccessibleText("שדה חיפוש לקוחות");
        searchField.setTooltip(new Tooltip("הקלד שם או מספר טלפון לחיפוש"));

        // Real-time search (on-type)
        searchField.textProperty().addListener((obs, old, newVal) -> filterClients());

        countLabel = new Label();
        countLabel.getStyleClass().add("client-count");

        HBox searchBar = new HBox(12, searchField, countLabel);
        searchBar.getStyleClass().add("search-bar");
        searchBar.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        searchBar.setAlignment(Pos.CENTER);

        // ---- Cards display ----
        cardDisplay = new VBox(12);
        cardDisplay.setPadding(new Insets(8));
        cardDisplay.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(cardDisplay);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ---- Navigation ----
        leftBtn = new Button("◀ הקודם");
        leftBtn.getStyleClass().add("page-btn");
        leftBtn.setTooltip(new Tooltip("עמוד קודם"));
        leftBtn.setAccessibleText("עמוד קודם");

        rightBtn = new Button("הבא ▶");
        rightBtn.getStyleClass().add("page-btn");
        rightBtn.setTooltip(new Tooltip("עמוד הבא"));
        rightBtn.setAccessibleText("עמוד הבא");

        pageLabel = new Label();
        pageLabel.getStyleClass().add("page-indicator");

        Button jumpBtn = new Button("🔢");
        jumpBtn.getStyleClass().add("page-btn");
        jumpBtn.setTooltip(new Tooltip("קפוץ לעמוד מסוים"));
        jumpBtn.setAccessibleText("קפוץ לעמוד");

        leftBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                showClientsPage();
            }
        });

        rightBtn.setOnAction(e -> {
            if ((currentPage + 1) * CLIENTS_PER_PAGE < clients.size()) {
                currentPage++;
                showClientsPage();
            }
        });

        jumpBtn.setOnAction(e -> {
            int totalPages = Math.max(1, (int) Math.ceil((double) clients.size() / CLIENTS_PER_PAGE));
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
                } catch (NumberFormatException ignored) {}
            });
        });

        HBox navigation = new HBox(8, rightBtn, pageLabel, leftBtn, jumpBtn);
        navigation.getStyleClass().add("navigation-bar");
        navigation.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        showClientsPage();

        VBox pane = new VBox(12, searchBar, scrollPane, navigation);
        pane.getStyleClass().add("clients-pane");
        pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // Auto-focus search field
        javafx.application.Platform.runLater(() -> searchField.requestFocus());

        return pane;
    }

    /** Legacy standalone stage method. */
    public void show(Stage stage) {
        stage.setTitle("ניהול לקוחות");
        Node content = createPane();

        Scene scene = new Scene(new StackPane(content), 600, 600);
        scene.getStylesheets().add(
                getClass().getClassLoader().getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void filterClients() {
        String keyword = searchField.getText().toLowerCase();

        clients = clientMap.values().stream()
                .filter(client ->
                        (client.firstName() + " " + client.lastName()).toLowerCase().contains(keyword)
                        || client.phoneNumber().toLowerCase().contains(keyword)
                )
                .collect(Collectors.toList());

        currentPage = 0;
        showClientsPage();
    }

    private void showClientsPage() {
        cardDisplay.getChildren().clear();

        countLabel.setText("סה\"כ: " + clients.size() + " לקוחות");

        if (clients.isEmpty()) {
            Label empty = new Label("🔍 לא נמצאו לקוחות.");
            empty.getStyleClass().add("client-count");
            empty.setStyle("-fx-font-size: 16px; -fx-padding: 40;");
            cardDisplay.getChildren().add(empty);
            pageLabel.setText("");
            leftBtn.setDisable(true);
            rightBtn.setDisable(true);
            return;
        }

        int start = currentPage * CLIENTS_PER_PAGE;
        int end = Math.min(start + CLIENTS_PER_PAGE, clients.size());
        List<Client> pageClients = clients.subList(start, end);

        for (Client client : pageClients) {
            cardDisplay.getChildren().add(createClientCard(client));
        }

        int totalPages = (int) Math.ceil((double) clients.size() / CLIENTS_PER_PAGE);
        pageLabel.setText("עמוד " + (currentPage + 1) + " / " + totalPages);

        leftBtn.setDisable(currentPage <= 0);
        rightBtn.setDisable((currentPage + 1) * CLIENTS_PER_PAGE >= clients.size());
    }

    private VBox createClientCard(Client client) {
        VBox card = new VBox(6);
        card.getStyleClass().add("client-card");

        String fullName = client.firstName() + " " + client.lastName();

        Label name = new Label("👤 " + fullName);
        name.getStyleClass().add("client-name");

        Label phone = new Label("📞 " + client.phoneNumber());
        phone.getStyleClass().add("client-detail");

        Label email = new Label("✉️ " + client.email());
        email.getStyleClass().add("client-detail");

        card.getChildren().addAll(name, phone, email);

        // Show experience info from bookings
        Booking booking = bookings.get(fullName);
        if (booking != null) {
            Label experience = new Label("🎯 ניסיון: " + booking.experience());
            experience.getStyleClass().add("client-detail");
            card.getChildren().add(experience);
        }

        card.setAccessibleText(fullName + ", טלפון: " + client.phoneNumber());
        Tooltip.install(card, new Tooltip(fullName + "\n📞 " + client.phoneNumber() + "\n✉️ " + client.email()));

        return card;
    }
}
