package com.bayoumi.controllers.home;

import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.SunnahTimes;
import com.bayoumi.Launcher;
import com.bayoumi.controllers.azkar.timed.TimedAzkarController;
import com.bayoumi.models.*;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.prayertimes.PrayerTimesUtil;
import com.bayoumi.util.services.azkar.AzkarService;
import com.bayoumi.util.services.reminders.ReminderService;
import com.bayoumi.util.time.ArabicNumeralDiscrimination;
import com.bayoumi.util.time.HijriDate;
import com.bayoumi.util.time.Utilities;
import com.bayoumi.util.validation.SingleInstance;
import com.jfoenix.controls.JFXButton;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class HomeController implements Initializable {

    // ==== Helper Objects ====
    private String remainingTime, currentPrayerValue;
    // ==== Settings Objects ====
    private AzkarSettings azkarSettings;
    private OtherSettings otherSettings;
    private PrayerTimeSettings prayerTimeSettings;
    private NotificationSettings notificationSettings;
    private PrayerTimes prayerTimesToday;
    // ==== GUI Objects ====
    @FXML
    private Label timeLabel, day, hijriDate, gregorianDate;
    @FXML
    private Label currentPrayerText, remainingTimeLabel;
    @FXML
    private HBox fajrBox, dhuhrBox, asrBox, maghribBox, ishaBox, currentPrayerBox;
    @FXML
    private Label fajrText, sunriseText, dhuhrText, asrText, maghribText, ishaText;
    @FXML
    private Label fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime;
    @FXML
    private VBox loadingBox, periodBox, sunnahBox;
    @FXML
    private Label middleOfTheNightTime, lastThirdOfTheNightTime;
    @FXML
    private Label frequencyLabel;
    @FXML
    private JFXButton highFrequency, midFrequency, lowFrequency, rearFrequency, currentFrequency;

    // ==============================================
    // ============ Prayer Times Methods ============
    // ==============================================
    @FXML
    private void openSunnahBox() {
        sunnahBox.setVisible(true);
    }

    @FXML
    private void removeSunnahBox() {
        sunnahBox.setVisible(false);
    }

    private void setPrayerTimesValuesToGUI(PrayerTimes prayerTimes, OtherSettings otherSettings) {
        Platform.runLater(() -> {
            SimpleDateFormat formatter;
            if (otherSettings.isEnable24Format()) {
                formatter = new SimpleDateFormat("HH:mm", new Locale(otherSettings.getLanguageLocal()));
            } else {
                formatter = new SimpleDateFormat("hh:mm a", new Locale(otherSettings.getLanguageLocal()));
            }
            formatter.setTimeZone(TimeZone.getDefault());

            fajrTime.setText(formatter.format(prayerTimes.fajr));
            sunriseTime.setText(formatter.format(prayerTimes.sunrise));
            dhuhrTime.setText(formatter.format(prayerTimes.dhuhr));
            asrTime.setText(formatter.format(prayerTimes.asr));
            maghribTime.setText(formatter.format(prayerTimes.maghrib));
            ishaTime.setText(formatter.format(prayerTimes.isha));

            SunnahTimes sunnahTimes = new SunnahTimes(prayerTimes);
            middleOfTheNightTime.setText(formatter.format(sunnahTimes.middleOfTheNight));
            lastThirdOfTheNightTime.setText(formatter.format(sunnahTimes.lastThirdOfTheNight));

            loadingBox.setVisible(false);
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildPopOver();

        // initialize required dependencies
        otherSettings = new OtherSettings();
        azkarSettings = new AzkarSettings();
        prayerTimeSettings = new PrayerTimeSettings();
        notificationSettings = new NotificationSettings();

        prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(prayerTimeSettings);
        setPrayerTimesValuesToGUI(prayerTimesToday, otherSettings);

        currentFrequency = getSelectedButton();
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        // if no azkar for notification terminate the program
        if (!AbsoluteZekr.fetchData()) {
            Launcher.workFine.setValue(false);
            return;
        }

        if (!azkarSettings.isStopped()) {
            AzkarService.init(this, notificationSettings);
        } else {
            periodBox.setDisable(true);
        }

        initClock();
        ReminderService.init(azkarSettings, prayerTimeSettings, notificationSettings);
        Date date = new Date();
        day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
        gregorianDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
        hijriDate.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));
    }

    private void buildPopOver() {
        //Build PopOver look and feel
        Label label = new Label("يمكن تعديل معدل ظهور الأذكار من الإعدادت");
        label.setStyle("-fx-padding: 10;-fx-background-color: #E9C46A;-fx-text-fill: #000000;-fx-font-weight: bold;");
        //Create PopOver and add look and feel
        PopOver popOver = new PopOver(label);
        frequencyLabel.setOnMouseEntered(mouseEvent -> {
            //Show PopOver when mouse enters label
            popOver.show(frequencyLabel);
        });
        popOver.setCloseButtonEnabled(true);
        frequencyLabel.setOnMouseExited(mouseEvent -> {
            //Hide PopOver when mouse exits label
            popOver.hide();
        });
    }

    private void changeCurrentPrayerBox(HBox box, String value) {
        if (currentPrayerBox != null && !currentPrayerBox.equals(box)) {
            currentPrayerBox.getStyleClass().remove("box-prayer-selected");
        }
        currentPrayerBox = box;
        currentPrayerValue = value;
    }

    private void timelineAction(PrayerTimes prayerTimesToday, Date date) {
        // get [ current prayer ] and [ next Prayer ]
        switch (prayerTimesToday.nextPrayer().equals(Prayer.NONE) ? prayerTimesToday.currentPrayer() : prayerTimesToday.nextPrayer()) {
            case FAJR:
                changeCurrentPrayerBox(fajrBox, "صلاة الفجر");
                break;
            case DHUHR:
            case SUNRISE:
                changeCurrentPrayerBox(dhuhrBox, "صلاة الظهر");
                break;
            case ASR:
                changeCurrentPrayerBox(asrBox, "صلاة العصر");
                break;
            case MAGHRIB:
                changeCurrentPrayerBox(maghribBox, "صلاة المغرب");
                break;
            case ISHA:
                changeCurrentPrayerBox(ishaBox, "صلاة العشاء");
                break;
        }
        if (currentPrayerBox != null && !currentPrayerBox.getStyleClass().contains("box-prayer-selected")) {
            currentPrayerBox.getStyleClass().add("box-prayer-selected");
        }

        Date nextPrayerTime;
        if (prayerTimesToday.nextPrayer().equals(Prayer.SUNRISE)) {
            // currentPrayerText.setText("متبقي على " + currentPrayerValue);
            currentPrayerText.setText(currentPrayerValue);
            nextPrayerTime = prayerTimesToday.dhuhr;
        }
        // take current Prayer ( when isha is finished and it's before 12pm )
        else if (prayerTimesToday.nextPrayer().equals(Prayer.NONE)) {
            // currentPrayerText.setText("مر على " + currentPrayerValue);
            currentPrayerText.setText(currentPrayerValue);
            nextPrayerTime = prayerTimesToday.timeForPrayer(prayerTimesToday.currentPrayer());
        } else {
            // currentPrayerText.setText("متبقي على " + currentPrayerValue);
            currentPrayerText.setText(currentPrayerValue);
            nextPrayerTime = prayerTimesToday.timeForPrayer(prayerTimesToday.nextPrayer());
        }

        if (date.compareTo(nextPrayerTime) < 0) {
            // date is before nextPrayerTime
            remainingTime = "- ";
        } else if (date.compareTo(nextPrayerTime) > 0) {
            // date is after nextPrayerTime
            remainingTime = "+ ";
        }
        remainingTime += Utilities.findDifference(date, nextPrayerTime);
//        System.out.println("remainingTime: " + remainingTime);
//        System.out.println("date: " + date);
        remainingTimeLabel.setText(remainingTime);
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            Date date = new Date();

            // if there is a change in HijriDate offset
            if (OtherSettings.isUpdated) {
                otherSettings.loadSettings();
                OtherSettings.isUpdated = false;
                hijriDate.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));
                setPrayerTimesValuesToGUI(prayerTimesToday, otherSettings);
            }

            // -- increment time --
            String timeNow;
            if (otherSettings.isEnable24Format()) {
                timeNow = Utilities.getTime24(otherSettings.getLanguageLocal(), date);
            } else {
                timeNow = Utilities.getTime(otherSettings.getLanguageLocal(), date);
            }
            timeLabel.setText(timeNow);

            // is new day? => change Dates and the prayer times
            if (timeNow.equals("12:00:00 AM") || timeNow.equals("12:00:00 ص") || timeNow.equals("00:00:00")) {
                // update week day name
                day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
                // update Gregorian date
                gregorianDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
                // update Hijri date
                hijriDate.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));
                // get prayer times for the new day
                prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(prayerTimeSettings);
                setPrayerTimesValuesToGUI(prayerTimesToday, otherSettings);
                // reinitialize the ReminderService
                ReminderService.init(azkarSettings, prayerTimeSettings, notificationSettings);
            }

            // if Notification Settings changed
            if (NotificationSettings.isUpdated) {
                notificationSettings.loadSettings();
                NotificationSettings.isUpdated = false;
            }
            // if time periods of Azkar and settings has changed
            if (AzkarSettings.isUpdated) {
                azkarSettings.loadSettings();
                AzkarSettings.isUpdated = false;
                if (AzkarService.absoluteAzkarTask != null) {
                    AzkarService.absoluteAzkarTask.stopTask();
                }
                if (azkarSettings.isStopped()) {
                    periodBox.setDisable(true);
                } else {
                    AzkarService.init(this, notificationSettings);
                    periodBox.setDisable(false);
                }
                ReminderService.init(azkarSettings, prayerTimeSettings, notificationSettings);
            }
            if (PrayerTimeSettings.isUpdated) {
                prayerTimeSettings.loadSettings();
                prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(prayerTimeSettings);
                setPrayerTimesValuesToGUI(prayerTimesToday, otherSettings);
                ReminderService.init(azkarSettings, prayerTimeSettings, notificationSettings);
                PrayerTimeSettings.isUpdated = false;
            }

            timelineAction(prayerTimesToday, date);
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

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
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/azkar/timed/TimedAzkar.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            TimedAzkarController controller = loader.getController();
            controller.setData(type);
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.show();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".showTimedAzkar()");
        }
    }

    @FXML
    private void goToSettings() {
        try {
//            Utility.printAllRunningThreads();
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/views/settings/Settings.fxml"))));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToSettings()");
        }
    }

    // ---------------------------------------------------------------------------
    // --------------------- Azkar Notification Init methods ---------------------
    // ---------------------------------------------------------------------------
    @FXML
    private void highFrequencyAction() {
        toggleFrequencyBTN(highFrequency);
    }

    @FXML
    private void midFrequencyAction() {
        toggleFrequencyBTN(midFrequency);
    }

    @FXML
    private void lowFrequencyAction() {
        toggleFrequencyBTN(lowFrequency);
    }

    @FXML
    private void rearFrequencyAction() {
        toggleFrequencyBTN(rearFrequency);
    }

    private void toggleFrequencyBTN(JFXButton b) {
        currentFrequency.getStyleClass().remove("frequency-btn-selected");
        currentFrequency = b;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        AzkarService.absoluteAzkarTask.updateTimer();
        setFrequencyLabel();
        azkarSettings.setSelectedPeriod(currentFrequency.getText());
        azkarSettings.saveSelectedPeriod();
    }

    private JFXButton getSelectedButton() {
        if (azkarSettings.getSelectedPeriod().equals(highFrequency.getText())) {
            return highFrequency;
        } else if (azkarSettings.getSelectedPeriod().equals(midFrequency.getText())) {
            return midFrequency;
        } else if (azkarSettings.getSelectedPeriod().equals(lowFrequency.getText())) {
            return lowFrequency;
        } else if (azkarSettings.getSelectedPeriod().equals(rearFrequency.getText())) {
            return rearFrequency;
        }
        return highFrequency;
    }

    public void setFrequencyLabel() {
        String msg = "ظهور كل" + " ";
        if (currentFrequency.equals(highFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(azkarSettings.getHighPeriod());
        } else if (currentFrequency.equals(midFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(azkarSettings.getMidPeriod());
        } else if (currentFrequency.equals(lowFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(azkarSettings.getLowPeriod());
        } else if (currentFrequency.equals(rearFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(azkarSettings.getRearPeriod());
        }
        frequencyLabel.setText(msg);
    }

    public Long getPeriod() {
        if (currentFrequency.equals(highFrequency)) {
            return azkarSettings.getHighPeriod() * 60000L;
        } else if (currentFrequency.equals(midFrequency)) {
            return azkarSettings.getMidPeriod() * 60000L;
        } else if (currentFrequency.equals(lowFrequency)) {
            return azkarSettings.getLowPeriod() * 60000L;
        } else if (currentFrequency.equals(rearFrequency)) {
            return azkarSettings.getRearPeriod() * 60000L;
        }
        return 300000L;
//        if (currentFrequency.equals(highFrequency)) {
//            return 3000L;
//        } else if (currentFrequency.equals(midFrequency)) {
//            return 30000L;
//        } else if (currentFrequency.equals(lowFrequency)) {
//            return 40000L;
//        } else if (currentFrequency.equals(rearFrequency)) {
//            return 50000L;
//        }
//        return 50000L;
    }

}
