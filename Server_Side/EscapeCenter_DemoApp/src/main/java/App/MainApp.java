package App;

import ui.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the EscapeCenter desktop application.
 * Launches the JavaFX login screen with dark theme.
 *
 * This class must not extend Application itself: Java refuses to start an
 * Application subclass from the Run arrow when JavaFX is on the classpath.
 */
public class MainApp {

    private static final int MIN_WINDOW_WIDTH = 500;
    private static final int MIN_WINDOW_HEIGHT = 400;

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    public static class App extends Application {

        @Override
        public void start(Stage primaryStage) {
            primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
            primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);

            new LoginScreen().show(primaryStage);
        }
    }
}
