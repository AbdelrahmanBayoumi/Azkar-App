package com.bayoumi.controllers.home;

import com.bayoumi.Launcher;
import com.bayoumi.controllers.azkar.timed.TimedAzkarController;
import com.bayoumi.models.*;
import com.bayoumi.util.EditablePeriodTimerTask;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utilities;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.prayertimes.PrayerTimesDBManager;
import com.bayoumi.util.prayertimes.PrayerTimesValidation;
import com.bayoumi.util.time.HijriDate;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalTime;
import java.util.Date;
import java.util.Random;
import java.util.ResourceBundle;

public class
HomeController implements Initializable {

    @FXML
    public Label hijriDate;
    @FXML
    public Label day;
    @FXML
    public Label gregorianDate;
    @FXML
    public Label timeLabel;
    @FXML
    private VBox periodBox;
    private EditablePeriodTimerTask absoluteAzkarTask;
    private OtherSettings otherSettings;
    private AzkarSettings azkarSettings;
    @FXML
    private Label frequencyLabel;
    @FXML
    private JFXButton highFrequency;
    @FXML
    private JFXButton midFrequency;
    @FXML
    private JFXButton lowFrequency;
    @FXML
    private JFXButton rearFrequency;
    private JFXButton currentFrequency;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
   /*
        //Build PopOver look and feel
        VBox vBox = new VBox();
        //Create PopOver and add look and feel
        PopOver popOver = new PopOver(vBox);
        frequencyLabel.setOnMouseEntered(mouseEvent -> {
            //Show PopOver when mouse enters label
            popOver.show(frequencyLabel);
        });

        frequencyLabel.setOnMouseExited(mouseEvent -> {
            //Hide PopOver when mouse exits label
            popOver.hide();
        });*/

        otherSettings = new OtherSettings();
        azkarSettings = new AzkarSettings();

        currentFrequency = getSelectedButton();
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        if (!AbsoluteZekr.fetchData()) {
            Launcher.workFine.setValue(false);
            return;
        }

        if (!azkarSettings.isStopped()) {
            initAzkarThread();
        } else {
            periodBox.setDisable(true);
        }

        initClock();
        initReminders();
        Date date = new Date();
        day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
        gregorianDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
        hijriDate.setText(HijriDate.getHijriDateNowString(otherSettings.getLanguageLocal()));
    }

    private void initReminders() {
        System.out.println("initReminders() ... ");

        AzkarReminderService.clearAllTasks();
        if (PrayerTimesValidation.PRAYERTIMES_STATUS.getValue() == 1) {
            Image morningImage = new Image("/com/bayoumi/images/sun_50px.png");
            Image nightImage = new Image("/com/bayoumi/images/night_50px.png");
            PrayerTimes prayerTimesForToday = PrayerTimesDBManager.getPrayerTimesForToday();
            if (azkarSettings.getMorningAzkarOffset() != 0) {
                AzkarReminderService.create(LocalTime.parse(prayerTimesForToday.getFajr()).plusMinutes(azkarSettings.getMorningAzkarOffset()).toString(), "أذكار الصباح", morningImage);
            }
            if (azkarSettings.getNightAzkarOffset() != 0) {
                AzkarReminderService.create(LocalTime.parse(prayerTimesForToday.getAsr()).plusMinutes(azkarSettings.getNightAzkarOffset()).toString(), "أذكار المساء", nightImage);
            }
        } else {
            PrayerTimesValidation.PRAYERTIMES_STATUS.addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() == 1) {
                    initReminders();
                }
            });
        }
    }

    private void initAzkarThread() {
        setFrequencyLabel("initAzkarThread");
        absoluteAzkarTask = null;
        absoluteAzkarTask = new EditablePeriodTimerTask(()
                -> {
            if (AbsoluteZekr.absoluteZekrObservableList.isEmpty()) {
                return;
            }
            Platform.runLater(()
                    -> Notification.create(
                    AbsoluteZekr.absoluteZekrObservableList.get(
                            new Random().nextInt(AbsoluteZekr.absoluteZekrObservableList.size())).getText(),
                    null));
        },
                this::getPeriod);
        absoluteAzkarTask.updateTimer();
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            Date date = new Date();
            String timeNow;
            if (OtherSettings.isUpdated) {
                otherSettings = new OtherSettings();
                OtherSettings.isUpdated = false;
            }
            if (otherSettings.isEnable24Format()) {
                timeNow = Utilities.getTime24(otherSettings.getLanguageLocal(), date);
            } else {
                timeNow = Utilities.getTime(otherSettings.getLanguageLocal(), date);
            }
            timeLabel.setText(timeNow);

            // Is it new day? => change Dates and the prayer times
            if (timeNow.equals("12:00:00 AM") || timeNow.equals("12:00:00 ص") || timeNow.equals("00:00:00")) {
                System.out.println("NEW DAY ...");
                day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
                hijriDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
                gregorianDate.setText(HijriDate.getHijriDateNowString(otherSettings.getLanguageLocal()));
            }

            if (AzkarSettings.isUpdated) {
                azkarSettings = new AzkarSettings();
                AzkarSettings.isUpdated = false;
                if (absoluteAzkarTask != null) {
                    absoluteAzkarTask.stopTask();
                }
                if (azkarSettings.isStopped()) {
                    periodBox.setDisable(true);
                } else {
                    initAzkarThread();
                    periodBox.setDisable(false);
                }
                initReminders();
            }
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
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

    private void setFrequencyLabel(String s) {
        System.out.println("S: " + s);
        String msg = "ظهور كل" + " ";
        if (currentFrequency.equals(highFrequency)) {
            msg += com.bayoumi.util.time.Utilities.getTimeArabicPlurality(azkarSettings.getHighPeriod());
        } else if (currentFrequency.equals(midFrequency)) {
            msg += com.bayoumi.util.time.Utilities.getTimeArabicPlurality(azkarSettings.getMidPeriod());
        } else if (currentFrequency.equals(lowFrequency)) {
            msg += com.bayoumi.util.time.Utilities.getTimeArabicPlurality(azkarSettings.getLowPeriod());
        } else if (currentFrequency.equals(rearFrequency)) {
            msg += com.bayoumi.util.time.Utilities.getTimeArabicPlurality(azkarSettings.getRearPeriod());
        }
        frequencyLabel.setText(msg);
    }

    private Long getPeriod() {
        if (currentFrequency.equals(highFrequency)) {
            return azkarSettings.getHighPeriod() * 60000L; // 5 min
        } else if (currentFrequency.equals(midFrequency)) {
            return azkarSettings.getMidPeriod() * 60000L; // 10 min
        } else if (currentFrequency.equals(lowFrequency)) {
            return azkarSettings.getLowPeriod() * 60000L; // 20 min
        } else if (currentFrequency.equals(rearFrequency)) {
            return azkarSettings.getRearPeriod() * 60000L; // 30 min
        }
        return 300000L; // 5 min

//        if (currentFrequency.equals(highFrequency)) {
//            return 15000L;
//        } else if (currentFrequency.equals(midFrequency)) {
//            return 30000L;
//        } else if (currentFrequency.equals(lowFrequency)) {
//            return 40000L;
//        } else if (currentFrequency.equals(rearFrequency)) {
//            return 50000L;
//        }
//        return 50000L;
    }

    @FXML
    private void goToMorningAzkar() {
        showTimedAzkar("morning");
    }

    @FXML
    private void goToNightAzkar() {
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
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".showTimedAzkar()");
        }
    }

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

        absoluteAzkarTask.updateTimer();
        setFrequencyLabel("toggleFrequencyBTN");
        azkarSettings.setSelectedPeriod(currentFrequency.getText());
        azkarSettings.saveSelectedPeriod();
    }

    @FXML
    private void goToPrayerTimes() {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/prayertimes/PrayerTimes.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToPrayerTimes()");
        }
    }

    @FXML
    private void goToSettings() {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/views/settings/Settings.fxml"))));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            stage.setResizable(false);
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToSettings()");
        }
    }

}
