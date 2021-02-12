package com.bayoumi;

import com.bayoumi.controllers.home.HomeController;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.preloader.CustomPreloaderMain;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utilities;
import com.bayoumi.util.db.DatabaseAssetsManager;
import com.bayoumi.util.db.DatabaseHandler;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.tray.TrayUtil;
import com.bayoumi.util.prayertimes.PrayerTimesValidation;
import com.bayoumi.util.validation.SingleInstance;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {
    // preloader flags
    public static final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    public static double preloaderProgress = 0;
    // for logging purpose
    public static Long startTime;
    // GUI Object
    public static HomeController homeController;
    // Program characteristics
    private final String VERSION = "1.0.4_3";
    // GUI Object
    private Scene scene = null;

    public static void main(String[] args) {
        LauncherImpl.launchApplication(Launcher.class, CustomPreloaderMain.class, args);
    }

    @Override
    public void stop() {
        System.out.println("stop()...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Long exitTime = System.currentTimeMillis();
            Logger.info("App closed - Used for "
                    + (exitTime - startTime) + " ms\n");
        }));
    }

    @Override
    public void init() {
        startTime = System.currentTimeMillis();
        // if anything in loading goes wrong => terminate the program
        workFine.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                Utilities.exitProgramAction();
            }
        });
        incrementPreloader();

        try {
            // Create Needed Folder if not exist.
            Utilities.createDirectory(Constants.assetsPath + "/logs");
            Utilities.createDirectory(Constants.assetsPath + "/db");
            Utilities.createDirectory(Constants.assetsPath + "/audio");

            Logger.init();
            Logger.info("App Launched");
            incrementPreloader();

            Constants.copyAssetsDBToAppData(); // TODO in Production remove comment for this line.

            DatabaseAssetsManager databaseAssetsManager = DatabaseAssetsManager.getInstance();
            if (!databaseAssetsManager.init()) {
                workFine.setValue(false);
            }
            // validate version
            databaseAssetsManager.setVersion(VERSION);
            incrementPreloader();

            // connect to db (READ ONLY DB)
            DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
            if (!databaseHandler.connectToDatabase()) {
                workFine.setValue(false);
            }
            incrementPreloader();

            // load  Homepage FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/home/home.fxml"));
            scene = new Scene(loader.load());
            scene.getStylesheets().add("/com/bayoumi/css/style.css");
            homeController = loader.getController();
            incrementPreloader();

            // Get Prayer Times
            new PrayerTimesValidation().start();


//            Thread.sleep(500);
//            incrementPreloader();
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
            ex.printStackTrace();
            workFine.setValue(false);
        }
    }

    /**
     * notify the Preloader to increment
     * the ProgressBar by 10%
     */
    private void incrementPreloader() {
        preloaderProgress += 0.1;
        LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(preloaderProgress));
    }

    public void start(Stage primaryStage) {
        // initialize tray icon
        new TrayUtil(primaryStage);
        // add loaded scene to primaryStage
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        // set Title and Icon to primaryStage
        HelperMethods.SetAppDecoration(primaryStage);
        OtherSettings otherSettings = new OtherSettings();
        if (!otherSettings.isMinimized()) {
            primaryStage.show();
        }
        // assign current primaryStage to SingleInstance Class
        SingleInstance.getInstance().setCurrentStage(primaryStage);
    }


}
