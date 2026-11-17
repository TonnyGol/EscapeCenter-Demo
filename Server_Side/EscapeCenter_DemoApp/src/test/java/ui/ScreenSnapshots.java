package ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import service.ApiService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Saves a PNG of every staff screen for before-and-after pictures. Run by hand, it is not a unit test:
 * <pre>mvnw test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
 * java -cp "target/classes;target/test-classes;$(cat target/cp.txt)" ui.ScreenSnapshots</pre>
 * Add the argument "min" to photograph the screens at the smallest window size (files end in "-min") instead of maximized.
 * (mvn exec:java hides this class's errors and hangs on JavaFX's thread, so it runs with plain java.)
 * Run it at most once a minute: the server limits requests per address and minute, and a second run answers 429.
 * The logged-in screens need the server running. With APP_ADMIN_USER / APP_ADMIN_PASSWORD in the environment it logs in
 * by itself, out of sight. Without them it shows the login window and waits for you to type and press login.
 */
public final class ScreenSnapshots {

    private static final Path OUTPUT_DIR = Path.of("../../screenshots");
    private static final String USER_VARIABLE = "APP_ADMIN_USER";
    private static final String PASSWORD_VARIABLE = "APP_ADMIN_PASSWORD";
    private static final String NAV_BUTTON_STYLE_CLASS = ".nav-button";
    private static final String DAY_CHIP_STYLE_CLASS = ".day-chip";
    private static final String MINIMUM_SIZE_ARGUMENT = "min";
    private static final String MINIMUM_SIZE_FILE_SUFFIX = "-min";
    private static final int BOOKINGS_NAV_INDEX = 1;
    private static final List<String> NAV_SCREEN_NAMES = List.of("dashboard", "bookings", "clients", "reports");
    private static final double OFFSCREEN_X = -5000; // the window renders fine out of sight
    private static final long SETTLE_MILLIS = 1500; // screens load their data in the background
    private static final long WAIT_FOR_LOGIN_MILLIS = 300_000;
    private static final long LOGIN_POLL_MILLIS = 500;

    private ScreenSnapshots() {
    }

    private static String fileSuffix = "";

    public static void main(String[] args) throws Exception {
        boolean atMinimumSize = args.length > 0 && MINIMUM_SIZE_ARGUMENT.equals(args[0]);
        fileSuffix = atMinimumSize ? MINIMUM_SIZE_FILE_SUFFIX : "";
        Files.createDirectories(OUTPUT_DIR);
        Platform.startup(() -> { });

        String user = System.getenv(USER_VARIABLE);
        String password = System.getenv(PASSWORD_VARIABLE);
        boolean typedByHand = user == null || password == null;

        Stage stage = onFxThread(() -> {
            Stage newStage = new Stage();
            if (!typedByHand) newStage.setX(OFFSCREEN_X);
            new LoginScreen().show(newStage);
            return newStage;
        });
        save(stage, "staff-login");

        if (typedByHand) {
            System.out.println("Type the username and password in the login window and press login. Waiting...");
            waitForNavigationShell(stage);
        } else if (ApiService.getInstance().login(user, password)) {
            onFxThread(() -> {
                new NavigationShell(stage).show();
                return null;
            });
        } else {
            System.out.println("The server refused " + USER_VARIABLE + " / " + PASSWORD_VARIABLE + " or is not running.");
            System.exit(1);
        }
        if (atMinimumSize) {
            onFxThread(() -> {
                stage.setMaximized(false); // the shell opens maximized; un-maximizing falls back to its minimum size
                return null;
            });
        }
        for (int index = 0; index < NAV_SCREEN_NAMES.size(); index++) {
            int navIndex = index;
            onFxThread(() -> {
                navButtons(stage).get(navIndex).fire();
                return null;
            });
            save(stage, "staff-" + NAV_SCREEN_NAMES.get(index));
            if (index == BOOKINGS_NAV_INDEX) saveEveryDayOfBookingsWeek(stage);
        }
        System.exit(0); // the bookings screen polls in the background
    }

    /** The all-rooms timeline shows one day at a time, so each day of the week gets its own picture. Runs on the bookings screen. */
    private static void saveEveryDayOfBookingsWeek(Stage stage) throws Exception {
        int dayCount = onFxThread(() -> stage.getScene().getRoot().lookupAll(DAY_CHIP_STYLE_CLASS).size());
        for (int day = 0; day < dayCount; day++) {
            int dayIndex = day;
            onFxThread(() -> {
                ((Button) List.copyOf(stage.getScene().getRoot().lookupAll(DAY_CHIP_STYLE_CLASS)).get(dayIndex)).fire();
                return null;
            });
            save(stage, "staff-bookings-day-" + (day + 1));
        }
    }

    private static void waitForNavigationShell(Stage stage) throws Exception {
        long giveUpAt = System.currentTimeMillis() + WAIT_FOR_LOGIN_MILLIS;
        while (onFxThread(() -> navButtons(stage).isEmpty())) {
            if (System.currentTimeMillis() > giveUpAt) {
                System.out.println("No login within " + WAIT_FOR_LOGIN_MILLIS / 1000 + " seconds, stopping.");
                System.exit(1);
            }
            Thread.sleep(LOGIN_POLL_MILLIS);
        }
    }

    private static List<Button> navButtons(Stage stage) {
        // The sidebar order is dashboard, bookings, clients, reports, then logout, which is never fired.
        return stage.getScene().getRoot().lookupAll(NAV_BUTTON_STYLE_CLASS).stream().map(node -> (Button) node).toList();
    }

    private static void save(Stage stage, String name) throws Exception {
        Thread.sleep(SETTLE_MILLIS);
        WritableImage snapshot = onFxThread(() -> stage.getScene().snapshot(null));
        PixelReader pixels = snapshot.getPixelReader();
        int width = (int) snapshot.getWidth();
        int height = (int) snapshot.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        ImageIO.write(image, "png", OUTPUT_DIR.resolve(name + fileSuffix + ".png").toFile());
        System.out.println("Saved " + name + fileSuffix + ".png (" + width + "x" + height + ")");
    }

    private static <T> T onFxThread(Supplier<T> work) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.get());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get();
    }
}
