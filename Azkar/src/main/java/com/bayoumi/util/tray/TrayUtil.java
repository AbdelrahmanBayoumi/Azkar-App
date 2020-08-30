package com.bayoumi.util.tray;

import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import javafx.application.Platform;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.rmi.CORBA.Util;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TrayUtil {

    private final Stage stage;
    private java.awt.SystemTray tray;
    private TrayIcon trayIcon;

    public TrayUtil(Stage stage) {
        // stores a reference to the stage.
        this.stage = stage;
        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);
        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);
        stage.setOnCloseRequest(event -> {
            this.stage.hide();
            event.consume();
        });
    }

    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                throw new Exception("No system tray support, application exiting.");
            }

            // set up a system tray icon.
            tray = java.awt.SystemTray.getSystemTray();
            BufferedImage trayIconImage = ImageIO.read(TrayUtil.class.getResource("/com/bayoumi/images/icons8_basilica_50px.png"));
            int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
            trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

            // setup the popup menu for the application.
            createPopupMenu();

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error("Unable to init system tray", ex, TrayUtil.class.getName() + ".addAppToTray()");
        }
    }


    /**
     * setup the popup menu for the application.
     */
    private void createPopupMenu() {
        // if the user selects the default menu item (which includes the app name),
        // show the main app stage.
        java.awt.MenuItem openItem = new java.awt.MenuItem("Open");
        openItem.addActionListener(event -> Platform.runLater(this::showStage));

        // the convention for tray icons seems to be to set the default icon for opening
        // the application stage in a bold font.
        java.awt.Font defaultFont = java.awt.Font.decode(null);
        java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
        openItem.setFont(boldFont);

        // to really exit the application, the user must go to the system tray icon
        // and select the exit option, this will shutdown JavaFX and remove the
        // tray icon (removing the tray icon will also shut down AWT).
        java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
        exitItem.addActionListener(event -> {
            tray.remove(trayIcon);
            Utility.exitProgramAction();
        });

        // setup the popup menu for the application.
        final java.awt.PopupMenu popup = new java.awt.PopupMenu();
        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);
    }


    /**
     * Shows the application stage and ensures that it is brought ot the front
     * of all stages.
     */
    private void showStage() {
        if (stage != null) {
            this.stage.show();
            this.stage.toFront();
        }
    }


}
