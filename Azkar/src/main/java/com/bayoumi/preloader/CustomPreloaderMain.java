package com.bayoumi.preloader;

import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.validation.SingleInstance;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CustomPreloaderMain extends Preloader {

    private Stage preloaderStage;
    private Scene scene;
    private SplashScreenController splashScreenController;

    public CustomPreloaderMain() {
    }

    @Override
    public void init() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.SplashScreen.getName()));
        Parent root = loader.load();
        splashScreenController = loader.getController();
        scene = new Scene(root);
        scene.getStylesheets().add("/com/bayoumi/css/preloader.css");
    }

    @Override
    public void start(Stage primaryStage) {
        this.preloaderStage = primaryStage;
        preloaderStage.setScene(scene);
        scene.setFill(Color.TRANSPARENT);
        preloaderStage.initStyle(StageStyle.UNDECORATED);
        preloaderStage.initStyle(StageStyle.TRANSPARENT);
        HelperMethods.SetAppDecoration(primaryStage);
        preloaderStage.show();
        preloaderStage.setOnCloseRequest((event) -> System.exit(0));
        SingleInstance.getInstance().setCurrentStage(primaryStage);
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            JFXProgressBar bar = splashScreenController.progressBarInstance;
            Label label = splashScreenController.progressLabel;
            double progress = ((ProgressNotification) info).getProgress();
            bar.setProgress(progress);
            label.setText("Loading " + (int) (progress * 100) + "%");
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            // Called after MyApplication#init and before MyApplication#start is called.
            preloaderStage.hide();
        }

    }
}
