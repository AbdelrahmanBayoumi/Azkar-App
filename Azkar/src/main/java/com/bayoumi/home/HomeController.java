package com.bayoumi.home;

import com.bayoumi.datamodel.AbsoluteZekr;
import com.bayoumi.util.EditablePeriodTimerTask;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.notfication.Notification;
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
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    private Timeline timeline_debug;
    private LocalTime time_debug = LocalTime.parse("00:00:00");
    private EditablePeriodTimerTask absoluteAzkarTask;
    @FXML
    private Label timeLabel;
    @FXML
    private StackPane sp;
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

    public static void main(String[] args) {
        Date date = new Date(); // Gregorian date

        Calendar cl = Calendar.getInstance();
        cl.setTime(date);

        HijrahDate islamicDate = HijrahChronology.INSTANCE.date(LocalDate.of(cl.get(Calendar.YEAR), cl.get(Calendar.MONTH) + 1, cl.get(Calendar.DATE)));
        System.out.println(islamicDate);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentFrequency = highFrequency;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        AbsoluteZekr.fetchData();
        absoluteAzkarTask = new EditablePeriodTimerTask(() ->
                Platform.runLater(() ->
                        Notification.create(
                                AbsoluteZekr.absoluteZekrObservableList.get(
                                        new Random().nextInt(AbsoluteZekr.absoluteZekrObservableList.size())).getText()
                                , null))
                , this::getPeriod);
        absoluteAzkarTask.updateTimer("initialize()");

        timeline_debug = new Timeline(new KeyFrame(Duration.millis(1000), ae -> {
            time_debug = time_debug.plusSeconds(1);
            System.out.println(time_debug.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        timeline_debug.setCycleCount(Animation.INDEFINITE);
        timeline_debug.play();

        initClock();
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a");
            timeLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private Long getPeriod() {
//        if (currentFrequency.equals(highFrequency)) {
//            return 900000L;
//        } else if (currentFrequency.equals(midFrequency)) {
//            return 1800000L;
//        } else if (currentFrequency.equals(lowFrequency)) {
//            return 3600000L;
//        } else if (currentFrequency.equals(rearFrequency)) {
//            return 7200000L;
//        }
//        return 900000L;

        if (currentFrequency.equals(highFrequency)) {
            return 5000L;
        } else if (currentFrequency.equals(midFrequency)) {
            return 20000L;
        } else if (currentFrequency.equals(lowFrequency)) {
            return 25000L;
        } else if (currentFrequency.equals(rearFrequency)) {
            return 30000L;
        }
        return 5000L;
    }


    @FXML
    private void goToMorningAzkar() {
        System.out.println("goToMorningAzkar");
    }

    @FXML
    private void goToNightAzkar() {
        System.out.println("goToNightAzkar");
    }

    @FXML
    private void goToPrayerTimes() {
        System.out.println("goToPrayerTimes");
    }

    @FXML
    private void goToSettings() {
        System.out.println("goToSettings");
    }

    @FXML
    private void highFrequencyAction() {
        String msg = "ظهور كل" + " " + 15 + " " + "دقيقة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(highFrequency);
    }

    @FXML
    private void midFrequencyAction() {
        String msg = "ظهور كل" + " " + 30 + " " + "دقيقة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(midFrequency);
    }

    @FXML
    private void lowFrequencyAction() {
        String msg = "ظهور كل" + " " + 1 + " " + "ساعة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(lowFrequency);
    }


    @FXML
    private void rearFrequencyAction() {
        String msg = "ظهور كل" + " " + 2 + " " + "ساعة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(rearFrequency);
    }

    private void toggleFrequencyBTN(JFXButton b) {
        currentFrequency.getStyleClass().remove("frequency-btn-selected");
        currentFrequency = b;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        timeline_debug.stop();
        time_debug = LocalTime.parse("00:00:00");
        timeline_debug.play();

        absoluteAzkarTask.updateTimer("toggle()");
    }

    @FXML
    private void goToAzkar() {
        System.out.println("goToAzkar() ..");
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/fxml/azkar/absolute/AbsoluteAzkar.fxml"))));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            Utility.SetIcon(stage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToAzkar()");
        }

    }
}
