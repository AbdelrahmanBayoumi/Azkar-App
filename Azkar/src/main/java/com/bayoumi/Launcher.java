package com.bayoumi;

import com.bayoumi.controllers.components.audio.ChooseAudioController;
import com.bayoumi.controllers.dialog.DownloadResourcesController;
import com.bayoumi.controllers.home.HomeController;
import com.bayoumi.models.Onboarding;
import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.preloader.CustomPreloaderMain;
import com.bayoumi.services.TimedAzkarService;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.SentryUtil;
import com.bayoumi.util.Utility;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.db.LocationsDBManager;
import com.bayoumi.util.file.FileUtils;
import com.bayoumi.util.gui.ArabicTextSupport;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.load.Loader;
import com.bayoumi.util.gui.load.LoaderComponent;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.gui.tray.TrayUtil;
import com.bayoumi.util.validation.SingleInstance;
import com.bayoumi.util.web.AzkarServer;
import com.install4j.api.launcher.StartupNotification;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;


public class Launcher extends Application {
    // preloader flags
    public static final SimpleBooleanProperty workFine = new SimpleBooleanProperty(true);
    public static double preloaderProgress = 0;
    private boolean locationsDBError = false;
    // for logging purpose
    public static Long startTime;
    // GUI Object
    public static HomeController homeController;
    // GUI Object
    private Scene scene = null;

    public static void main(String[] args) {
        ArabicTextSupport.fix();
        LauncherImpl.launchApplication(Launcher.class, CustomPreloaderMain.class, args);
    }

    @Override
    public void stop() {
        Logger.debug("stop()...");
        Utility.exitProgramAction();
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
            FileUtils.createDirectory(Constants.assetsPath + "/logs");
            FileUtils.createDirectory(Constants.assetsPath + "/db");
            FileUtils.createDirectory(Constants.assetsPath + "/audio/adhan");
            FileUtils.createDirectory(Constants.assetsPath + "/azkar");

            // To save the audio file in the temp directory to be able to play it
            FileUtils.createDirectory(System.getProperty("java.io.tmpdir") + "/" + Constants.APP_NAME);
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

            // --- initialize Preferences ---
            Preferences.init();
            incrementPreloader();

            // --- initialize database connection (locationsDB) ---
            try {
                LocationsDBManager.getInstance();
            } catch (Exception ex) {
                locationsDBError = true;
            }
            incrementPreloader();

            // --- load Homepage FXML ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.Home.toString()));
            scene = new Scene(loader.load());
            scene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            homeController = loader.getController();
            incrementPreloader();
            // --- initialize Sentry for error tracking ---
            try {
                SentryUtil.init();
            } catch (Exception ex) {
                Logger.debug(ex.getLocalizedMessage());
            }
            incrementPreloader();
            AzkarServer.init();
            incrementPreloader();

            TimedAzkarService.init();
            incrementPreloader();
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

    private void handleLocationDBError() {
        if (!locationsDBError) return;

        try {
            final LoaderComponent popUp = Loader.getInstance().getPopUp(Locations.DownloadResources);
            ((DownloadResourcesController) popUp.getController())
                    .setData(Constants.LOCATIONS_DB_URL, "jarFiles/db/locations.db", "locationsDBErrorInDownload", popUp.getStage(), Utility::exitProgramAction);
            popUp.showAndWait();
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".start() => show locationsDB download");
        }
    }

    private void showOnboardingIfFirstTimeOpened(boolean isFirstTimeOpened) {
        if (!isFirstTimeOpened) return;

        try {
            final Scene onboardingScene = new Scene(FXMLLoader.load(Objects.requireNonNull(getClass().getResource(Locations.Onboarding.toString()))));
            onboardingScene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            final Stage onboardingStage = BuilderUI.initStageDecorated(onboardingScene, "Onboarding - " + Constants.APP_NAME);
            onboardingStage.show();
            onboardingStage.setOnCloseRequest(event -> ChooseAudioController.stopIfPlaying());
            Preferences.getInstance().set(PreferencesType.APP_VERSION, Constants.VERSION);
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + "start() => show Onboarding stage");
        }
    }

    private void showVersionInstalled(boolean isFirstTimeOpened, boolean isNewVersion) {
        if (isFirstTimeOpened || !isNewVersion){
            return;
        }

        try {
            final Scene versionScene = new Scene(FXMLLoader.load(Objects.requireNonNull(getClass().getResource(Locations.VersionInstalled.toString()))));
            versionScene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            final Stage stage = BuilderUI.initStageDecorated(versionScene, Constants.APP_NAME + " - " + Constants.VERSION);
            stage.show();
            Preferences.getInstance().set(PreferencesType.APP_VERSION, Constants.VERSION);
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + "start() => show VersionInstalled stage");
        }
    }

    public void start(Stage primaryStage) throws Exception {
        handleLocationDBError();
        // initialize tray icon
        try {
            new TrayUtil(primaryStage);
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + "new TrayUtil()");
        }
        // add loaded scene to primaryStage
        primaryStage.setScene(scene);

        // set Title and Icon to primaryStage
        HelperMethods.SetAppDecoration(primaryStage);

        // show primaryStage
        final boolean isFirstTimeOpened = Onboarding.isFirstTimeOpened();
        final boolean isNewVersion = !Constants.VERSION.equals(Preferences.getInstance().get(PreferencesType.APP_VERSION));
        if (isFirstTimeOpened || isNewVersion || !Settings.getInstance().getMinimized()) {
            primaryStage.show();
        }

        // assign current primaryStage to SingleInstance Class
        SingleInstance.getInstance().setCurrentStage(primaryStage);

        // show Onboarding stage
        showOnboardingIfFirstTimeOpened(isFirstTimeOpened);

        // show VersionInstalled stage
        showVersionInstalled(isFirstTimeOpened, isNewVersion);

        StartupNotification.registerStartupListener(s ->
                SingleInstance.getInstance().openCurrentStage());
    }
}