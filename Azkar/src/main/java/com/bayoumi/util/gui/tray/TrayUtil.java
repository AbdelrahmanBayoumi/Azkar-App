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
 * Manages system tray integration across platforms with a platform-specific split:
 * <ul>
 *   <li><b>Windows:</b> Uses {@link java.awt.SystemTray} and {@link java.awt.TrayIcon}
 *       to support native single left-click window restore and right-click context menu.</li>
 *   <li><b>Non-Windows:</b> Uses Dorkbox {@link dorkbox.systemTray.SystemTray} (AutoDetect backend)
 *       to provide a working icon and menu on supported desktop environments such as Linux Mint (Cinnamon).</li>
 * </ul>
 * <p>
 * Usage: {@code TrayUtil.init(primaryStage);}
 */
public class TrayUtil {

    private final Stage stage;
    private volatile SystemTray tray; // dorkbox tray
    private volatile java.awt.SystemTray awtTray; // AWT tray
    private java.awt.TrayIcon awtTrayIcon;

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

        // Hide window on close only if tray is ready; otherwise fall back to exiting
        stage.setOnCloseRequest(event -> {
            if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                boolean trayNotReady = com.bayoumi.util.OSUtil.isWindows() ? instance.awtTray == null : instance.tray == null;
                if (Platform.isImplicitExit() || trayNotReady) {
                    // Tray failed to init or is not ready yet — fall back to normal exit behavior
                    instance.shutdown();
                    Utility.exitProgramAction();
                } else {
                    stage.hide();
                    event.consume();
                }
            }
        });

        // Initialize tray on a background thread to avoid blocking the JavaFX UI thread
        Thread initThread = new Thread(() -> {
            try {
                instance.setupTray();
                // Prevent JavaFX from exiting when the last window is closed once tray is ready
                Platform.setImplicitExit(false);
            } catch (Exception e) {
                Logger.error("Failed to initialize system tray",
                        e, TrayUtil.class.getName() + ".init()");
                // If tray fails, allow normal window close to exit the app
                instance.shutdown();
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
        if (com.bayoumi.util.OSUtil.isWindows()) {
            setupAwtTray();
            Logger.info("[TrayUtil] AWT system tray initialized successfully.");
        } else {
            setupDorkboxTray();
            Logger.info("[TrayUtil] Dorkbox system tray initialized successfully.");
        }
    }

    private void setupAwtTray() throws Exception {
        java.awt.Toolkit.getDefaultToolkit();

        if (!java.awt.SystemTray.isSupported()) {
            throw new Exception("No system tray support (AWT), falling back to standard window.");
        }

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Exception> error = new java.util.concurrent.atomic.AtomicReference<>();

        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                awtTray = java.awt.SystemTray.getSystemTray();
                java.awt.image.BufferedImage trayIconImage = javax.imageio.ImageIO.read(
                    java.util.Objects.requireNonNull(TrayUtil.class.getResource("/com/bayoumi/images/logo_50x50.png")));

                int trayIconWidth = new java.awt.TrayIcon(trayIconImage).getSize().width;
                awtTrayIcon = new java.awt.TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, java.awt.Image.SCALE_SMOOTH));
                awtTrayIcon.setToolTip(Constants.APP_NAME + " App");

                awtTrayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                            Platform.runLater(TrayUtil.this::showStage);
                        }
                    }
                });

                java.awt.MenuItem openItem = new java.awt.MenuItem("Open");
                openItem.addActionListener(event -> Platform.runLater(this::showStage));
                openItem.setFont(java.awt.Font.decode(null).deriveFont(java.awt.Font.BOLD));

                java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
                exitItem.addActionListener(event -> {
                    Logger.debug(event);
                    shutdown();
                    Platform.runLater(Utility::exitProgramAction);
                });

                java.awt.PopupMenu popup = new java.awt.PopupMenu();
                popup.add(openItem);
                popup.addSeparator();
                popup.add(exitItem);
                awtTrayIcon.setPopupMenu(popup);

                awtTray.add(awtTrayIcon);
            } catch (Exception ex) {
                error.set(ex);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        if (error.get() != null) {
            throw error.get();
        }
    }

    private void setupDorkboxTray() throws Exception {
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
        if (com.bayoumi.util.OSUtil.isWindows()) {
            if (awtTray != null && awtTrayIcon != null) {
                awtTray.remove(awtTrayIcon);
            }
        } else {
            SystemTray localTray = tray; // read volatile once
            if (localTray != null) {
                localTray.shutdown();
            }
        }
    }

    private void showStage() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }
}
