package com.bayoumi.util.gui.tray;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.net.URL;

/**
 * Manages system tray integration using dorkbox SystemTray.
 * Supports Linux (AppIndicator/GTK via JNA), Windows, and macOS.
 * <p>
 * Usage: {@code TrayUtil.init(primaryStage);}
 */
public class TrayUtil {

    private final Stage stage;
    private volatile SystemTray tray; // written on init thread, read on FX thread

    private TrayUtil(Stage stage) {
        this.stage = stage;
    }

    /**
     * Initializes the system tray for the given stage.
     * <p>
     * - Prevents JavaFX implicit exit so the app stays alive in the tray.
     * - Registers a close handler that hides the window instead of exiting.
     * - Launches tray setup on a background daemon thread to avoid blocking the UI.
     *
     * @param stage the primary application stage
     * @return the TrayUtil instance (can be stored for future shutdown calls)
     */
    public static TrayUtil init(Stage stage) {
        TrayUtil instance = new TrayUtil(stage);

        // Prevent JavaFX from exiting when the last window is closed
        Platform.setImplicitExit(false);

        // Hide window on close instead of exiting; the tray keeps the app alive
        stage.setOnCloseRequest(event -> {
            if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                if (Platform.isImplicitExit()) {
                    // Tray failed to init — fall back to normal exit behavior
                    instance.shutdown();
                    Utility.exitProgramAction();
                }
                stage.hide();
                event.consume();
            }
        });

        // Initialize tray on a background thread to avoid blocking the JavaFX UI thread
        Thread initThread = new Thread(() -> {
            try {
                instance.setupTray();
            } catch (Throwable e) {
                Logger.error("Failed to initialize system tray",
                        e, TrayUtil.class.getName() + ".init()");
                // If tray fails, allow normal window close to exit the app
                Platform.setImplicitExit(true);
            }
        }, "Tray-Init-Thread");
        initThread.setDaemon(true);
        initThread.start();

        return instance;
    }

    /**
     * Sets up the tray icon, tooltip, and menu.
     */
    private void setupTray() throws Exception {
        Logger.debug("[TrayUtil] Calling SystemTray.get()...");
        tray = SystemTray.get();
        if (tray == null) {
            throw new Exception("SystemTray not supported on this platform.");
        }
        Logger.debug("[TrayUtil] SystemTray instance: " + tray.getClass().getName());

        URL imageUrl = TrayUtil.class.getResource("/com/bayoumi/images/logo_50x50.png");
        if (imageUrl == null) {
            throw new Exception("Tray icon image not found.");
        }

        tray.setImage(imageUrl);
        tray.setTooltip(Constants.APP_NAME + " App");

        buildMenu();

        Logger.info("[TrayUtil] System tray initialized successfully.");
    }

    /**
     * Builds the tray popup menu with Open and Exit actions.
     */
    private void buildMenu() {
        tray.getMenu().add(new MenuItem("Open", e -> {
            Logger.info("[TrayUtil] Menu 'Open' clicked.");
            Platform.runLater(this::showStage);
        }));

        tray.getMenu().add(new Separator());

        tray.getMenu().add(new MenuItem("Exit", e -> {
            Logger.info("[TrayUtil] Menu 'Exit' clicked.");
            shutdown();
            Platform.runLater(Utility::exitProgramAction);
        }));
    }

    /**
     * Shuts down the system tray. Safe to call from any thread.
     */
    public void shutdown() {
        SystemTray localTray = tray; // read volatile once
        if (localTray != null) {
            localTray.shutdown();
        }
    }

    private void showStage() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }
}
