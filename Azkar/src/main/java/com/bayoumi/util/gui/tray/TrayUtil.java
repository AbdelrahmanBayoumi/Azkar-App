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

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Manages system tray integration using the platform-appropriate backend.
 * <p>
 * Usage: {@code TrayUtil.init(primaryStage);}
 */
public class TrayUtil {

    private enum Backend {
        NONE,
        DORKBOX,
        AWT
    }

    private final Stage stage;
    private final Object trayLock = new Object();
    private volatile Backend backend = Backend.NONE;
    private volatile SystemTray tray;
    private volatile java.awt.SystemTray awtTray;
    private volatile java.awt.TrayIcon awtTrayIcon;
    private volatile boolean trayReady;
    private volatile boolean shutdownRequested;

    private TrayUtil(Stage stage) {
        this.stage = stage;
    }

    /**
     * Initializes the system tray for the given stage.
     * <p>
     * - Prevents JavaFX implicit exit so the app stays alive in the tray.
     * - Registers a close handler that hides the window instead of exiting.
     * - Launches tray setup off the JavaFX Application Thread on the appropriate platform thread.
     *
     * @param stage the primary application stage
     * @return the TrayUtil instance (can be stored for future shutdown calls)
     */
    public static TrayUtil init(Stage stage) {
        TrayUtil instance = new TrayUtil(stage);
        instance.trayReady = false;

        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win") || osName.contains("linux")) {
            Platform.setImplicitExit(false);
            instance.installSupportedCloseHandler();
        }

        if (osName.contains("win")) {
            instance.initWindowsTray();
        } else if (osName.contains("linux")) {
            instance.initLinuxTray();
        } else {
            Platform.setImplicitExit(true);
        }

        return instance;
    }

    /**
     * Sets up the Linux tray icon, tooltip, and menu.
     */
    private void initLinuxTray() {
        Thread initThread = new Thread(() -> {
            try {
                setupLinuxTray();
            } catch (Exception e) {
                Logger.error("Failed to initialize system tray",
                        e, TrayUtil.class.getName() + ".init()");
                shutdown();
                Platform.setImplicitExit(true);
            }
        }, "Tray-Init-Thread");
        initThread.setDaemon(true);
        initThread.start();
    }

    private void setupLinuxTray() throws Exception {
        if (shutdownRequested) {
            return;
        }

        SystemTray localTray = null;
        boolean committed = false;
        try {
            Logger.debug("[TrayUtil] Calling SystemTray.get()...");
            localTray = SystemTray.get();
            if (localTray == null) {
                if (shutdownRequested) {
                    return;
                }
                throw new Exception("SystemTray not supported on this platform.");
            }
            Logger.debug("[TrayUtil] SystemTray instance: " + localTray.getClass().getName());

            if (shutdownRequested) {
                cleanupLinuxTray(localTray);
                return;
            }

            URL imageUrl = TrayUtil.class.getResource("/com/bayoumi/images/logo_50x50.png");
            if (imageUrl == null) {
                throw new Exception("Tray icon image not found.");
            }

            localTray.setImage(imageUrl);
            localTray.setTooltip(Constants.APP_NAME + " App");

            buildLinuxMenu(localTray);

            boolean shouldCleanup = false;
            synchronized (trayLock) {
                if (shutdownRequested) {
                    shouldCleanup = true;
                } else {
                    tray = localTray;
                    backend = Backend.DORKBOX;
                    trayReady = true;
                    committed = true;
                }
            }

            if (shouldCleanup) {
                cleanupLinuxTray(localTray);
                return;
            }

            Logger.info("[TrayUtil] System tray initialized successfully.");
        } catch (Exception e) {
            if (localTray != null && !committed) {
                cleanupLinuxTray(localTray);
            }
            throw e;
        }
    }

    /**
     * Sets up the Windows tray icon, tooltip, and menu.
     */
    private void initWindowsTray() {
        SwingUtilities.invokeLater(() -> {
            try {
                setupWindowsTray();
            } catch (Exception e) {
                Logger.error("Failed to initialize system tray",
                        e, TrayUtil.class.getName() + ".init()");
                shutdown();
                Platform.setImplicitExit(true);
            }
        });
    }

    private void setupWindowsTray() throws Exception {
        if (shutdownRequested) {
            return;
        }

        java.awt.Toolkit.getDefaultToolkit();
        if (!java.awt.SystemTray.isSupported()) {
            throw new Exception("SystemTray not supported on this platform.");
        }

        URL imageUrl = TrayUtil.class.getResource("/com/bayoumi/images/logo_50x50.png");
        if (imageUrl == null) {
            throw new Exception("Tray icon image not found.");
        }

        BufferedImage trayIconImage = ImageIO.read(imageUrl);
        if (trayIconImage == null) {
            throw new Exception("Tray icon image not found.");
        }

        java.awt.TrayIcon localTrayIcon = new java.awt.TrayIcon(trayIconImage);
        localTrayIcon.setImageAutoSize(true);
        localTrayIcon.setToolTip(Constants.APP_NAME + " App");
        localTrayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    Platform.runLater(TrayUtil.this::showStage);
                }
            }
        });

        java.awt.PopupMenu popup = new java.awt.PopupMenu();
        java.awt.MenuItem openItem = new java.awt.MenuItem("Open");
        openItem.addActionListener(event -> Platform.runLater(this::showStage));
        openItem.setFont(java.awt.Font.decode(null).deriveFont(java.awt.Font.BOLD));
        popup.add(openItem);
        popup.addSeparator();
        java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
        exitItem.addActionListener(event -> {
            shutdown();
            Platform.runLater(Utility::exitProgramAction);
        });
        popup.add(exitItem);
        localTrayIcon.setPopupMenu(popup);

        java.awt.SystemTray localTray = java.awt.SystemTray.getSystemTray();
        boolean iconAdded = false;
        boolean committed = false;
        try {
            localTray.add(localTrayIcon);
            iconAdded = true;

            boolean shouldCleanup = false;
            synchronized (trayLock) {
                if (shutdownRequested) {
                    shouldCleanup = true;
                } else {
                    awtTray = localTray;
                    awtTrayIcon = localTrayIcon;
                    backend = Backend.AWT;
                    trayReady = true;
                    committed = true;
                }
            }

            if (shouldCleanup) {
                cleanupWindowsTrayIcon(localTray, localTrayIcon);
                return;
            }

            Logger.info("[TrayUtil] System tray initialized successfully.");
        } catch (Exception e) {
            if (iconAdded && !committed) {
                cleanupWindowsTrayIcon(localTray, localTrayIcon);
            }
            throw e;
        }
    }

    /**
     * Builds the Linux tray popup menu with Open and Exit actions.
     */
    private void buildLinuxMenu(SystemTray localTray) {
        localTray.getMenu().add(new MenuItem("Open", e -> Platform.runLater(this::showStage)));
        localTray.getMenu().add(new Separator());
        localTray.getMenu().add(new MenuItem("Exit", e -> {
            shutdown();
            Platform.runLater(Utility::exitProgramAction);
        }));
    }

    private void cleanupLinuxTray(SystemTray localTray) {
        try {
            localTray.shutdown();
        } catch (Exception cleanupException) {
            Logger.warn("Failed to clean up Linux tray after initialization failure", cleanupException);
        }
    }

    private void cleanupWindowsTrayIcon(java.awt.SystemTray localTray, java.awt.TrayIcon localTrayIcon) {
        try {
            localTray.remove(localTrayIcon);
        } catch (Exception cleanupException) {
            Logger.warn("Failed to remove Windows tray icon after initialization failure", cleanupException);
        }
    }

    private void installSupportedCloseHandler() {
        stage.setOnCloseRequest(event -> {
            if (event.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                if (trayReady) {
                    stage.hide();
                    event.consume();
                    return;
                }
                shutdown();
                Utility.exitProgramAction();
                event.consume();
            }
        });
    }

    /**
     * Shuts down the system tray. Safe to call from any thread.
     */
    public void shutdown() {
        SystemTray localDorkboxTray = null;
        java.awt.SystemTray localAwtTray = null;
        java.awt.TrayIcon localAwtTrayIcon = null;

        synchronized (trayLock) {
            shutdownRequested = true;
            trayReady = false;

            if (backend == Backend.DORKBOX) {
                localDorkboxTray = tray;
                tray = null;
                backend = Backend.NONE;
            } else if (backend == Backend.AWT) {
                localAwtTray = awtTray;
                localAwtTrayIcon = awtTrayIcon;
                awtTray = null;
                awtTrayIcon = null;
                backend = Backend.NONE;
            }
        }

        Platform.setImplicitExit(true);

        if (localDorkboxTray != null) {
            cleanupLinuxTray(localDorkboxTray);
        }

        if (localAwtTray != null && localAwtTrayIcon != null) {
            cleanupWindowsTrayIcon(localAwtTray, localAwtTrayIcon);
        }
    }

    private void showStage() {
        if (stage != null) {
            stage.show();
            stage.toFront();
        }
    }
}
