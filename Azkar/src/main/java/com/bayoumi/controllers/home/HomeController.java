package com.bayoumi.controllers.home;

import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.bayoumi.Launcher;
import com.bayoumi.controllers.azkar.timed.TimedAzkarController;
import com.bayoumi.controllers.home.periods.AzkarPeriodsController;
import com.bayoumi.controllers.home.prayertimes.PrayerTimesController;
import com.bayoumi.models.azkar.AbsoluteZekr;
import com.bayoumi.models.azkar.TimedZekrDTO;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.services.TimedAzkarService;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.load.Loader;
import com.bayoumi.util.gui.load.LoaderComponent;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.gui.notfication.NotificationAudio;
import com.bayoumi.util.gui.notfication.NotificationContent;
import com.bayoumi.util.prayertimes.PrayerTimesUtil;
import com.bayoumi.util.services.azkar.AzkarService;
import com.bayoumi.util.services.reminders.Reminder;
import com.bayoumi.util.services.reminders.ReminderUtil;
import com.bayoumi.util.time.HijriDate;
import com.bayoumi.util.time.Utilities;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // ==== Helper Objects ====
    public Date date;
    private String remainingTime;
    private AzkarPeriodsController azkarPeriodsController;
    private PrayerTimesController prayerTimesController;
    private ResourceBundle bundle;
    // ==== Settings Objects ====
    private Settings settings;
    private PrayerTimes prayerTimesToday;
    // ==== GUI Objects ====
    @FXML
    private Label timeLabel, day, hijriDate, gregorianDate;
    @FXML
    private Label currentPrayerText, remainingTimeLabel, timeNow;
    @FXML
    private VBox container, periodBox, clockBox;
    @FXML
    private StackPane stackPane;
    @FXML
    private Button morningAzkarButton, nightAzkarButton, settingsButton;
    @FXML
    private HBox mainContainer, remainingTimeBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        LanguageBundle.getInstance().addObserver((o, arg) ->
                updateBundle(LanguageBundle.getInstance().getResourceBundle()));

        FXMLLoader fxmlLoader;
        date = Utilities.getCurrentDate();
        // initialize required dependencies
        settings = Settings.getInstance();
        prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(settings.getPrayerTimeSettings(), date);

        try {
            fxmlLoader = new FXMLLoader(getClass().getResource(Locations.PrayerTimes.toString()));
            mainContainer.getChildren().add(0, fxmlLoader.load());
            prayerTimesController = fxmlLoader.getController();
            prayerTimesController.setData(prayerTimesToday);
        } catch (Exception ex) {
            Logger.error("loading PrayerTimes", ex, getClass().getName() + ".initialize()");
            Launcher.workFine.setValue(false);
        }

        // if no azkar for notification terminate the program
        if (!AbsoluteZekr.fetchData()) {
            Launcher.workFine.setValue(false);
            return;
        }

        initClock();
        day.setText(Utilities.getDay(settings.getLanguage().getLocale(), date));
        gregorianDate.setText(Utilities.getGregorianDate(settings.getLanguage().getLocale(), date));
        hijriDate.setText(new HijriDate(settings.getHijriOffset()).getString(settings.getLanguage().getLocale()));

        initReminders();

        try {
            fxmlLoader = new FXMLLoader(getClass().getResource(Locations.AzkarPeriods.toString()));
            periodBox = fxmlLoader.load();
            azkarPeriodsController = fxmlLoader.getController();
            azkarPeriodsController.setData();
            container.getChildren().add(2, periodBox);
        } catch (Exception ex) {
            Logger.error("loading AzkarPeriods", ex, getClass().getName() + ".initialize()");
            Launcher.workFine.setValue(false);
        }

        settings.addObserver(PreferencesType.HIJRI_OFFSET, (key, value) ->
                hijriDate.setText(new HijriDate(settings.getHijriOffset()).getString(settings.getLanguage().getLocale())));

        settings.addObserver(PreferencesType.ENABLE_24_FORMAT, (key, value) ->
                prayerTimesController.setPrayerTimesValuesToGUI());

        // if time periods of Azkar and settings has changed
        settings.getAzkarSettings().addObserver((o, arg) -> {
            if (settings.getAzkarSettings().isStopped()) {
                AzkarService.stopService();
                periodBox.setDisable(true);
            } else {
                AzkarService.stopService();
                AzkarService.init(azkarPeriodsController);
                periodBox.setDisable(false);
            }
            // reinitialize Reminders
            initReminders();
        });

        settings.getPrayerTimeSettings().addObserver((o, arg) -> {
            prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(settings.getPrayerTimeSettings(), date);
            prayerTimesController.setPrayerTimes(prayerTimesToday);
            initReminders();
        });
    }

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        final String dir = Utility.toUTF(bundle.getString("dir"));
        remainingTimeBox.setNodeOrientation(NodeOrientation.valueOf(dir));
        clockBox.setNodeOrientation(NodeOrientation.valueOf(dir));
        timeNow.setText(Utility.toUTF(bundle.getString("time.now")));
        morningAzkarButton.setText(Utility.toUTF(bundle.getString("morningAzkar")));
        nightAzkarButton.setText(Utility.toUTF(bundle.getString("nightAzkar")));
        settingsButton.setText(Utility.toUTF(bundle.getString("settings")));
    }


    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            date = Utilities.getCurrentDate();

            // -- increment time --
            String timeNow;
            final String locale = settings.getLanguage().getLocale();
            if (settings.getEnable24Format()) {
                timeNow = Utilities.getTime24(locale, date);
            } else {
                timeNow = Utilities.getTime(locale, date);
            }
            timeLabel.setText(timeNow);

            // is new day? => change Dates and the prayer times
            if (timeNow.equals("12:00:00 AM") || timeNow.equals("12:00:00 ص") || timeNow.equals("00:00:00")
                    || !Utilities.getDay(locale, date).equals(day.getText())) {
                // update week day name
                day.setText(Utilities.getDay(locale, date));
                // update Gregorian date
                gregorianDate.setText(Utilities.getGregorianDate(locale, date));
                // update Hijri date
                hijriDate.setText(new HijriDate(settings.getHijriOffset()).getString(locale));
                // get prayer times for the new day
                prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(settings.getPrayerTimeSettings(), date);
                prayerTimesController.setPrayerTimes(prayerTimesToday);
                // reinitialize Reminders
                initReminders();
            }

            prayerTimesController.prayerTimelineAction(date);
            handlePrayerRemainingTime(date);
            checkForReminders(date);
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }


    // ==============================================
    // ============== Buttons Handler ===============
    // ==============================================
    @FXML
    public void goToMorningAzkar() {
        showTimedAzkar("morning");
    }

    @FXML
    public void goToNightAzkar() {
        showTimedAzkar("night");
    }

    private void showTimedAzkar(String type) {
        try {
            List<TimedZekrDTO> timedZekrDTOList;
            try {
                timedZekrDTOList = TimedAzkarService.getTimedAzkar(settings.getLanguage().getLocale(), type.equals("morning") ? 1 : 2);
            } catch (Exception ex) {
                try {
                    TimedAzkarService.downloadLatestReleaseFiles();
                    timedZekrDTOList = TimedAzkarService.getTimedAzkar(settings.getLanguage().getLocale(), type.equals("morning") ? 1 : 2);
                } catch (Exception e) {
                    return;
                }
            }

            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(Locations.TimedAzkar.toString()));
            final Stage stage = BuilderUI.initStageDecorated(new Scene(fxmlLoader.load()), Utility.toUTF(bundle.getString(type + "Azkar")));
            ((TimedAzkarController) fxmlLoader.getController()).setData(timedZekrDTOList, type, stage);
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".showTimedAzkar()");
        }
    }

    @FXML
    private void goToSettings() {
        try {
            final LoaderComponent popUp = Loader.getInstance().getPopUp(Locations.Settings);
            HelperMethods.ExitKeyCodeCombination(popUp.getStage().getScene(), popUp.getStage());
            popUp.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToSettings()");
        }
    }


    // ==============================================
    // ============ Prayer Times Methods ============
    // ==============================================
    private void handlePrayerRemainingTime(Date dateNow) {
        if (bundle != null) {
            // take current Prayer ( when Isha is finished, and it's before 12pm )
            if (prayerTimesToday.nextPrayer(dateNow).equals(Prayer.NONE)) {
                currentPrayerText.setText(Utility.toUTF(bundle.getString("havePassedSince")) + " " + prayerTimesController.getCurrentPrayerValue());
            } else if (prayerTimesToday.nextPrayer(dateNow).equals(prayerTimesController.getCurrentPrayer())) {
                currentPrayerText.setText(Utility.toUTF(bundle.getString("leftFor")) + " " + prayerTimesController.getCurrentPrayerValue());
            } else {
                currentPrayerText.setText(Utility.toUTF(bundle.getString("havePassedSince")) + " " + prayerTimesController.getCurrentPrayerValue());
            }
            final Date nextPrayerTime = prayerTimesToday.timeForPrayer(prayerTimesController.getCurrentPrayer());

            if (dateNow.compareTo(nextPrayerTime) < 0) {
                // dateNow is before nextPrayerTime
                remainingTime = "- ";
            } else if (dateNow.compareTo(nextPrayerTime) > 0) {
                // dateNow is after nextPrayerTime
                remainingTime = "+ ";
            }
            remainingTime += Utilities.findDifference(dateNow, nextPrayerTime);
            remainingTimeLabel.setText(remainingTime);
        }
    }

    private void initReminders() {
        ReminderUtil.getInstance().clear();
        ReminderUtil.getInstance().add(new Reminder(prayerTimesToday.fajr, () -> playAdhan("صلاة الفجر")));
        ReminderUtil.getInstance().add(new Reminder(prayerTimesToday.dhuhr, () -> playAdhan("صلاة الظهر")));
        ReminderUtil.getInstance().add(new Reminder(prayerTimesToday.asr, () -> playAdhan("صلاة العصر")));
        ReminderUtil.getInstance().add(new Reminder(prayerTimesToday.maghrib, () -> playAdhan("صلاة المغرب")));
        ReminderUtil.getInstance().add(new Reminder(prayerTimesToday.isha, () -> playAdhan("صلاة العشاء")));
        if (settings.getAzkarSettings().getMorningAzkarOffset() != 0) {
            Date morningAzkarDate = ((Date) prayerTimesToday.fajr.clone());
            morningAzkarDate.setTime(prayerTimesToday.fajr.getTime() + (settings.getAzkarSettings().getMorningAzkarOffset() * 60000L));
            ReminderUtil.getInstance().add(new Reminder(morningAzkarDate, () -> Platform.runLater(() ->
                    Notification.create(new NotificationContent("أذكار الصباح",
                                    new Image("/com/bayoumi/images/sun_50px.png")),
                            30,
                            settings.getNotificationSettings().getPosition(),
                            () -> Launcher.homeController.goToMorningAzkar(),
                            new NotificationAudio(settings.getAzkarSettings().getAudioName(), settings.getAzkarSettings().getVolume())))));
        }
        if (settings.getAzkarSettings().getNightAzkarOffset() != 0) {
            Date nightAzkarDate = ((Date) prayerTimesToday.asr.clone());
            nightAzkarDate.setTime(prayerTimesToday.asr.getTime() + (settings.getAzkarSettings().getNightAzkarOffset() * 60000L));
            ReminderUtil.getInstance().add(new Reminder(nightAzkarDate, () -> Platform.runLater(() ->
                    Notification.create(new NotificationContent("أذكار المساء",
                                    new Image("/com/bayoumi/images/night_50px.png")),
                            30,
                            settings.getNotificationSettings().getPosition(),
                            () -> Launcher.homeController.goToNightAzkar(),
                            new NotificationAudio(settings.getAzkarSettings().getAudioName(), settings.getAzkarSettings().getVolume())))));
        }
    }

    private void checkForReminders(Date date) {
        ReminderUtil.getInstance().validate(date);
    }

    private void playAdhan(String prayerName) {
        Logger.debug("playAdhan() => " + prayerName);
        String adhanFileName = Settings.getInstance().getPrayerTimeSettings().getAdhanAudio();
        if (adhanFileName == null || adhanFileName.isEmpty()) {
            adhanFileName = "";
        } else {
            adhanFileName = "adhan/" + adhanFileName;
        }
        String finalAdhanFileName = adhanFileName;
        Platform.runLater(() -> Notification.create(new NotificationContent(prayerName,
                        new Image("/com/bayoumi/images/Kaaba.png")),
                240,
                settings.getNotificationSettings().getPosition(),
                null,
                new NotificationAudio(finalAdhanFileName, 100)));
    }

}
