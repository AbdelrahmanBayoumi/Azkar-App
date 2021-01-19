package com.bayoumi.controllers.home;

import com.bayoumi.Launcher;
import com.bayoumi.controllers.azkar.timed.TimedAzkarController;
import com.bayoumi.models.AbsoluteZekr;
import com.bayoumi.models.AzkarSettings;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.util.EditablePeriodTimerTask;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utilities;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.notfication.Notification;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
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

//        timeline_debug = new Timeline(new KeyFrame(Duration.millis(1000), ae -> {
//            time_debug = time_debug.plusSeconds(1);
//            System.out.println(time_debug.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
//        }));
//        timeline_debug.setCycleCount(Animation.INDEFINITE);
//        timeline_debug.play();
        initClock();

        Date date = new Date();
        day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
        gregorianDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
        hijriDate.setText(HijriDate.getHijriDateNowString(otherSettings.getLanguageLocal()));
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
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".showTimedAzkar()");
        }
    }

    private void setFrequencyLabel(String s) {
        System.out.println("S: " + s);
        String msg = "";
        if (currentFrequency.equals(highFrequency)) {
            msg = "ظهور كل" + " " + azkarSettings.getHighPeriod() + " " + "دقائق";
        } else if (currentFrequency.equals(midFrequency)) {
            msg = "ظهور كل" + " " + azkarSettings.getMidPeriod() + " " + "دقائق";
        } else if (currentFrequency.equals(lowFrequency)) {
            msg = "ظهور كل" + " " + azkarSettings.getLowPeriod() + " " + "دقيقة";
        } else if (currentFrequency.equals(rearFrequency)) {
            msg = "ظهور كل" + " " + azkarSettings.getRearPeriod() + " " + "دقيقة";
        }
        frequencyLabel.setText(msg);
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

//        timeline_debug.stop();
//        time_debug = LocalTime.parse("00:00:00");
//        timeline_debug.play();

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
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToPrayerTimes()");
        }
    }

    @FXML
    private void goToSettings() {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/settings/Settings.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToSettings()");
        }
    }

}
