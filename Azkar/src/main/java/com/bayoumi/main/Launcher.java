package com.bayoumi.main;

import com.bayoumi.home.HomeController;
import com.bayoumi.preloader.CustomPreloader;
import com.bayoumi.util.DatabaseHandler;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Launcher extends Application {
    public static double preloaderProgress = 0;
    public static Long startTime;
    private final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    private Parent root = null;
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
            root = loader.load();
            HomeController homeController = loader.getController();
            scene = new Scene(root);
            scene.getStylesheets().add("/com/bayoumi/style/style.css");
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
            workFine.setValue(false);
        }
    }

    public void start(Stage primaryStage) {

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getWidth() - (500) - 30);
        primaryStage.setY(bounds.getHeight()  - (340) - 50);
        primaryStage.setScene(scene);
        StackPane r = (StackPane) scene.getRoot();
        System.out.println(r.getPrefWidth());
        System.out.println(r.getPrefHeight());
        Utility.SetAppDecoration(primaryStage);
        primaryStage.show();
    }
}
