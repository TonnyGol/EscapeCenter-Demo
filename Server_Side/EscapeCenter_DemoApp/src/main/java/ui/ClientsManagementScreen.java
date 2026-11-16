package ui;

import model.Booking;
import model.Client;
import service.ApiService;
import service.ServerMessages;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Clients management screen — real-time searchable, paginated client list
 * with dark-themed cards. Returns a pane for embedding in NavigationShell.
 */
public class ClientsManagementScreen {

    private List<Client> clients;
    private Map<String, Client> clientMap = new HashMap<>();
    private VBox cardDisplay;
    private Label pageLabel;
    private Label countLabel;
    private TextField searchField;
    private Button leftBtn;
    private Button rightBtn;

    private static final int CLIENTS_PER_PAGE = 10;
    private static final String NO_CLIENTS_MESSAGE = "לא נמצאו לקוחות. נסו שם או מספר טלפון אחר.";
    private static final String NO_EMAIL_LABEL = "אין אימייל";
    private static final double NAME_ICON_SIZE = 16;
    private static final double DETAIL_ICON_SIZE = 14;
    private static final String WHATSAPP_URL = "https://wa.me/";
    private static final String MAILTO_SCHEME = "mailto";
    private static final String ISRAEL_COUNTRY_CODE = "972";
    private static final String LOCAL_PHONE_PREFIX = "0";
    private static final String NON_DIGITS_REGEX = "\\D";
    private static final String SAVE_FAILED_TITLE = "השמירה נכשלה";
    private static final String CANNOT_OPEN_TITLE = "הקישור לא נפתח";
    private static final String HISTORY_LOAD_FAILED_TITLE = "ההיסטוריה לא נטענה";
    private static final String CANNOT_OPEN_MESSAGE = "לא ניתן לפתוח את הקישור במחשב הזה.";
    private static final String HISTORY_LOAD_FAILED_MESSAGE = "לא ניתן לטעון את היסטוריית ההזמנות. בדקו את החיבור לשרת ונסו שוב.";
    private static final String NO_BOOKINGS_MESSAGE = "ללקוח הזה אין הזמנות.";
    private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final double HISTORY_WIDTH = 760;
    private static final double HISTORY_HEIGHT = 380;
    private int currentPage = 0;

    /**
     * Creates the clients pane for embedding inside NavigationShell.
     */
    public Node createPane() {
        // Load data from server
        clientMap = new HashMap<>(ApiService.getInstance().loadClients());
        clients = new ArrayList<>(clientMap.values());

        // ---- Search bar ----
        searchField = new TextField();
        searchField.setPromptText("חיפוש לקוח לפי שם או טלפון...");
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

        Button jumpBtn = new Button("מעבר לעמוד");
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
            Label empty = new Label(NO_CLIENTS_MESSAGE);
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

        Label name = Icons.label(fullName, Icons.USER, NAME_ICON_SIZE);
        name.getStyleClass().add("client-name");

        Label phone = Icons.label(client.phoneNumber(), Icons.PHONE, DETAIL_ICON_SIZE);
        phone.getStyleClass().add("client-detail");

        Label email = Icons.label(hasEmail(client) ? client.email() : NO_EMAIL_LABEL, Icons.MAIL, DETAIL_ICON_SIZE);
        email.getStyleClass().add("client-detail");

        Button historyBtn = createCardButton("היסטוריית הזמנות", "כל ההזמנות של " + fullName);
        historyBtn.setOnAction(e -> showBookingHistory(client));

        Button editBtn = createCardButton("עריכה", "עריכת הפרטים של " + fullName);
        editBtn.setOnAction(e -> editClient(client));

        Button whatsAppBtn = createCardButton("וואטסאפ", "פתיחת שיחת וואטסאפ עם " + fullName);
        whatsAppBtn.setOnAction(e -> openLink(URI.create(whatsAppLink(client.phoneNumber()))));

        Button emailBtn = createCardButton("אימייל", "כתיבת אימייל אל " + fullName);
        emailBtn.setDisable(!hasEmail(client));
        emailBtn.setOnAction(e -> openEmail(client.email()));

        HBox actions = new HBox(8, historyBtn, editBtn, whatsAppBtn, emailBtn);
        actions.getStyleClass().add("client-actions");

        card.getChildren().addAll(name, phone, email, actions);

        card.setAccessibleText(fullName + ", טלפון: " + client.phoneNumber());
        Tooltip.install(card, new Tooltip(fullName + "\nטלפון: " + client.phoneNumber() + "\nאימייל: "
                + (hasEmail(client) ? client.email() : NO_EMAIL_LABEL)));

        return card;
    }

    private static boolean hasEmail(Client client) {
        return client.email() != null && !client.email().isBlank();
    }

    private Button createCardButton(String text, String description) {
        Button button = new Button(text);
        button.getStyleClass().add("page-btn");
        button.setTooltip(new Tooltip(description));
        button.setAccessibleText(description);
        return button;
    }

    /** WhatsApp wants the number in international form without "+": 050-123 4567 becomes 972501234567. */
    static String whatsAppLink(String phoneNumber) {
        String digits = phoneNumber.replaceAll(NON_DIGITS_REGEX, "");
        if (digits.startsWith(LOCAL_PHONE_PREFIX)) {
            digits = ISRAEL_COUNTRY_CODE + digits.substring(LOCAL_PHONE_PREFIX.length());
        }
        return WHATSAPP_URL + digits;
    }

    private void openEmail(String email) {
        try {
            // The three-part constructor escapes characters that are legal in an address but not in a URI.
            openLink(new URI(MAILTO_SCHEME, email, null));
        } catch (Exception e) {
            showError(CANNOT_OPEN_TITLE, CANNOT_OPEN_MESSAGE);
        }
    }

    /** Opens the link in the default browser or mail program of this computer. */
    private void openLink(URI link) {
        try {
            if (MAILTO_SCHEME.equals(link.getScheme())) {
                Desktop.getDesktop().mail(link);
            } else {
                Desktop.getDesktop().browse(link);
            }
        } catch (Exception e) {
            System.err.println("[Clients] Could not open " + link.getScheme() + " link: " + e.getMessage());
            showError(CANNOT_OPEN_TITLE, CANNOT_OPEN_MESSAGE);
        }
    }

    private void showError(String title, String reason) {
        Alert alert = new Alert(Alert.AlertType.ERROR, reason);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void editClient(Client client) {
        TextField firstNameField = new TextField(client.firstName());
        firstNameField.setAccessibleText("שם פרטי");
        TextField lastNameField = new TextField(client.lastName());
        lastNameField.setAccessibleText("שם משפחה");
        TextField phoneField = new TextField(client.phoneNumber());
        phoneField.setAccessibleText("טלפון");
        TextField emailField = new TextField(client.email());
        emailField.setAccessibleText("אימייל");

        GridPane form = new GridPane();
        form.getStyleClass().add("booking-form");
        form.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        form.add(new Label("שם פרטי:"), 0, 0);   form.add(firstNameField, 1, 0);
        form.add(new Label("שם משפחה:"), 0, 1);  form.add(lastNameField, 1, 1);
        form.add(new Label("טלפון:"), 0, 2);     form.add(phoneField, 1, 2);
        form.add(new Label("אימייל:"), 0, 3);    form.add(emailField, 1, 3);

        Label formError = new Label();
        formError.getStyleClass().add("form-error");
        form.add(formError, 1, 4);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("עריכת לקוח");
        dialog.getDialogPane().setContent(form);
        ButtonType saveType = new ButtonType("שמור שינויים", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, new ButtonType("סגור", ButtonBar.ButtonData.CANCEL_CLOSE));

        FormChecks.guardSave(dialog, saveType, firstNameField, lastNameField, phoneField, emailField, formError);

        if (dialog.showAndWait().filter(saveType::equals).isEmpty()) {
            return;
        }
        // An empty email box means "no email", which the server accepts; an empty string it does not.
        String email = emailField.getText() == null || emailField.getText().isBlank() ? null : emailField.getText().trim();
        Client edited = new Client(client.id(), firstNameField.getText(), lastNameField.getText(), phoneField.getText(), email);
        ApiService.getInstance().updateClient(edited).ifPresent(reason -> showError(SAVE_FAILED_TITLE, ServerMessages.inHebrew(reason)));
        clientMap = new HashMap<>(ApiService.getInstance().loadClients());
        int pageBeforeReload = currentPage;
        filterClients();
        currentPage = Math.min(pageBeforeReload, Math.max(0, (clients.size() - 1) / CLIENTS_PER_PAGE));
        showClientsPage();
    }

    private void showBookingHistory(Client client) {
        Optional<List<Booking>> history = ApiService.getInstance().loadClientBookings(client.id());
        if (history.isEmpty()) {
            showError(HISTORY_LOAD_FAILED_TITLE, HISTORY_LOAD_FAILED_MESSAGE);
            return;
        }

        TableView<Booking> table = new TableView<>();
        table.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(NO_BOOKINGS_MESSAGE));
        table.getColumns().add(historyColumn("ההזמנה נוצרה", booking -> formatDateTime(booking.createdAt())));
        table.getColumns().add(historyColumn("תאריך המשחק", booking -> formatDateTime(booking.startAt())));
        table.getColumns().add(historyColumn("חדר", Booking::roomName));
        table.getColumns().add(historyColumn("הערות הלקוח", Booking::notes));
        table.getColumns().add(historyColumn("הערות פנימיות", Booking::internalNotes));
        table.getItems().addAll(history.get());
        table.setPrefSize(HISTORY_WIDTH, HISTORY_HEIGHT);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("היסטוריית הזמנות");
        dialog.setHeaderText(client.fullName() + " — " + history.get().size() + " הזמנות");
        dialog.setResizable(true);
        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("סגור", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.showAndWait();
    }

    private static TableColumn<Booking, String> historyColumn(String title, Function<Booking, String> text) {
        TableColumn<Booking, String> column = new TableColumn<>(title);
        column.setCellValueFactory(row -> new SimpleStringProperty(text.apply(row.getValue())));
        column.setSortable(false);
        return column;
    }

    private static String formatDateTime(String isoDateTime) {
        return isoDateTime == null ? "" : LocalDateTime.parse(isoDateTime).format(HISTORY_DATE_FORMAT);
    }
}
