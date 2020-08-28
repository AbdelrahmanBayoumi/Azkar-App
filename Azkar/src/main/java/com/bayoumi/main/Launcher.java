package com.bayoumi.main;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;

import com.bayoumi.preloader.CustomPreloader;
import com.bayoumi.util.DatabaseHandler;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Launcher extends Application {
    private static final String iconImageLoc
            = "http://icons.iconarchive.com/icons/scafer31000/bubble-circle-3/16/GameCenter-icon.png";
    public static double preloaderProgress = 0;
    public static Long startTime;
    private final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    private Scene scene = null;
    private Stage stage;

    public static void main(String[] args) {
        LauncherImpl.launchApplication(Launcher.class, CustomPreloader.class, args);
    }

    @Override
    public void stop() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Long exitTime = System.currentTimeMillis();
            Logger.info("App closed - Used for "
                    + (exitTime - startTime) + " ms\n");
        }));
    }

    @Override
    public void init() {
        workFine.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                Utility.exitProgramAction();
            }
        });
        startTime = System.currentTimeMillis();
        Logger.init();
        Logger.info("App Launched");
        preloaderProgress += 0.1;
        LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(preloaderProgress));
        // connect to db
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        if (!databaseHandler.connectToDatabase()) {
            workFine.setValue(false);
        }
        preloaderProgress += 0.1;
        LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(preloaderProgress));
        try {
            // load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/fxml/home/home.fxml"));
            Parent root = loader.load();
//            HomeController homeController = loader.getController();
            scene = new Scene(root);
            scene.getStylesheets().add("/com/bayoumi/style/style.css");
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
            workFine.setValue(false);
        }
    }


    public void start(Stage primaryStage) {
        // stores a reference to the stage.
        this.stage = primaryStage;
        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);

        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);

        primaryStage.setOnCloseRequest(event -> {
            primaryStage.hide();
            event.consume();
        });

        Utility.setStageToBottom(primaryStage, 500, 340);
        primaryStage.setScene(scene);
        StackPane r = (StackPane) scene.getRoot();
        System.out.println(r.getPrefWidth());
        System.out.println(r.getPrefHeight());
        Utility.SetAppDecoration(primaryStage);
        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.show();
    }

    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
            }

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            URL imageLoc = new URL(
                    iconImageLoc
            );
//            java.awt.Image image = ImageIO.read(imageLoc);
            java.awt.Image image = Toolkit.getDefaultToolkit().createImage(Launcher.class.getResource("/com/bayoumi/images/icons8_basilica_50px.png"));
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

            // if the user selects the default menu item (which includes the app name),
            // show the main app stage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("Open App");
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
                Platform.exit();
                tray.remove(trayIcon);
                System.exit(0);
            });

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray");
            e.printStackTrace();
        }
    }

    /**
     * Shows the application stage and ensures that it is brought ot the front
     * of all stages.
     */
    private void showStage() {
        if (stage != null) {

            stage.show();
            stage.toFront();
        }
    }
}
