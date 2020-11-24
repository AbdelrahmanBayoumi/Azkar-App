package com.bayoumi;

import com.bayoumi.preloader.CustomPreloaderMain;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utilities;
import com.bayoumi.util.db.DatabaseHandler;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.tray.TrayUtil;
import com.bayoumi.util.validation.SingleInstance;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {

    public static final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    public static double preloaderProgress = 0;
    public static Long startTime;
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
        // Create Needed Folder if not exist.
        Utilities.createDirectory("jarFiles/logs");
        Utilities.createDirectory("jarFiles/db");
        Utilities.createDirectory("jarFiles/audio");

        Logger.init();
        Logger.info("App Launched");

        incrementPreloader();
        // connect to db
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        if (!databaseHandler.connectToDatabase()) {
            workFine.setValue(false);
        }
        incrementPreloader();
        try {
            // load FXML
            scene = new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/views/home/home.fxml")));
            scene.getStylesheets().add("/com/bayoumi/css/style.css");
            incrementPreloader();
            long tempTime = 200;
//            Thread.sleep(tempTime);
//            incrementPreloader();
//            Thread.sleep(tempTime);
//            incrementPreloader();
//            Thread.sleep(tempTime);
//            incrementPreloader();
//            Thread.sleep(tempTime);
//            incrementPreloader();
//            Thread.sleep(tempTime);
//            incrementPreloader();
//            Thread.sleep(tempTime);
//            incrementPreloader();
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
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
        primaryStage.show();
        // assign current primaryStage to SingleInstance Class
        SingleInstance.getInstance().setCurrentStage(primaryStage);
    }


}
