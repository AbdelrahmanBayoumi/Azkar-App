package com.bayoumi.main;

import com.bayoumi.preloader.CustomPreloader;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.db.DatabaseHandler;
import com.bayoumi.util.tray.TrayUtil;
import com.bayoumi.util.validation.SingleInstance;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Launcher extends Application {

    public static final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    public static double preloaderProgress = 0;
    public static Long startTime;
    private Scene scene = null;


    public static void main(String[] args) {
        LauncherImpl.launchApplication(Launcher.class, CustomPreloader.class, args);
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

    private void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    @Override
    public void init() {
        workFine.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                Utility.exitProgramAction();
            }
        });

        createDirectory("jarFiles/logs");
        createDirectory("jarFiles/db");
        createDirectory("jarFiles/audio");

        incrementPreloader();
        startTime = System.currentTimeMillis();

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
            scene = new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/fxml/home/home.fxml")));
            scene.getStylesheets().add("/com/bayoumi/style/style.css");
            incrementPreloader();
//            Thread.sleep(1500);
//            incrementPreloader();
//            Thread.sleep(1500);
//            incrementPreloader();
//            Thread.sleep(1500);
//            incrementPreloader();
//            Thread.sleep(1500);
//            incrementPreloader();
//            Thread.sleep(1500);
            incrementPreloader();
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
            workFine.setValue(false);
        }
    }

    private void incrementPreloader() {
        preloaderProgress += 0.1;
        LauncherImpl.notifyPreloader(this, new Preloader.ProgressNotification(preloaderProgress));
    }

    public void start(Stage primaryStage) {
        // initialize tray icon
        new TrayUtil(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        Utility.SetAppDecoration(primaryStage);
        primaryStage.show();
        SingleInstance.getInstance().setCurrentStage(primaryStage);
    }


}
