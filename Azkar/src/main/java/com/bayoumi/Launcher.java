package com.bayoumi;

import com.bayoumi.controllers.components.audio.ChooseAudioController;
import com.bayoumi.controllers.home.HomeController;
import com.bayoumi.models.Onboarding;
import com.bayoumi.models.settings.OtherSettings;
import com.bayoumi.preloader.CustomPreloaderMain;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.db.LocationsDBManager;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.tray.TrayUtil;
import com.bayoumi.util.validation.SingleInstance;
import com.sun.javafx.application.LauncherImpl;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class Launcher extends Application {
    // preloader flags
    public static final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    public static double preloaderProgress = 0;
    // for logging purpose
    public static Long startTime;
    // GUI Object
    public static HomeController homeController;
    // GUI Object
    private Scene scene = null;

    public static void main(String[] args) {
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.lcdtext", "false");
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
                Utility.exitProgramAction();
            }
        });
        incrementPreloader();

        try {
            // --- Create Needed Folder if not exist ---
            Utility.createDirectory(Constants.assetsPath + "/logs");
            Utility.createDirectory(Constants.assetsPath + "/db");
            Utility.createDirectory(Constants.assetsPath + "/audio");
            incrementPreloader();

            // --- initialize Logger ---
            Logger.init();
            Logger.info("App Launched");
            incrementPreloader();

            // --- initialize database connection ---
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            if (!databaseManager.init()) {
                workFine.setValue(false);
            }
            incrementPreloader();

            // --- initialize database connection (locationsDB) ---
            LocationsDBManager.getInstance();
            incrementPreloader();

            // --- load Homepage FXML ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/home/home.fxml"));
            scene = new Scene(loader.load());
            scene.getStylesheets().add("/com/bayoumi/css/style.css");
            homeController = loader.getController();
            incrementPreloader();
            // --- initialize Sentry for error tracking ---
            try {
                Sentry.init(options -> {
                    options.setEnableExternalConfiguration(true);
                    options.setTracesSampleRate(0.2);
                    options.setDiagnosticLevel(SentryLevel.DEBUG);
                    options.setDebug(true);
                    options.setRelease("azkar.app@1.0.0+1");
                });
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
            }
            incrementPreloader();

//            incrementPreloader();
//            incrementPreloader();
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
        // set Title and Icon to primaryStage
        HelperMethods.SetAppDecoration(primaryStage);
        if (Onboarding.isFirstTimeOpened() || !OtherSettings.getIsMinimizedDB()) {
            primaryStage.show();
        }
        // assign current primaryStage to SingleInstance Class
        SingleInstance.getInstance().setCurrentStage(primaryStage);

        // show Onboarding stage
        if (Onboarding.isFirstTimeOpened()) {
            try {
                Stage onboardingStage = new Stage();
                onboardingStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/views/onboarding/Onboarding.fxml"))));
                onboardingStage.initModality(Modality.APPLICATION_MODAL);
                HelperMethods.SetIcon(onboardingStage);
                onboardingStage.setTitle("Onboarding - Azkar");
                onboardingStage.show();
                onboardingStage.setOnCloseRequest(event -> ChooseAudioController.stopIfPlaying());
            } catch (Exception ex) {
                Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + "start() => show Onboarding stage");
                ex.printStackTrace();
            }
        }
        com.install4j.api.launcher.StartupNotification.registerStartupListener(s ->
                SingleInstance.getInstance().openCurrentStage());
    }


}
