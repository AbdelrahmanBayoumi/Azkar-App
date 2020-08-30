package com.bayoumi.main;

import com.bayoumi.preloader.CustomPreloader;
import com.bayoumi.util.db.DatabaseHandler;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.tray.TrayUtil;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {

    public static double preloaderProgress = 0;
    public static Long startTime;
    private final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    private Scene scene = null;


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
        // initialize tray icon
        new TrayUtil(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        Utility.SetAppDecoration(primaryStage);
        primaryStage.show();
    }


}
