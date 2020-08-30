package com.bayoumi.home;

import com.bayoumi.util.EditablePeriodTimerTask;
import com.bayoumi.util.notfication.Notification;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {
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
    private EditablePeriodTimerTask absoluteAzkarTask;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentFrequency = highFrequency;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        sp.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
//                        Timer timer = new Timer();
//                        TimerTask absoluteAzkarTask = new TimerTask() {
//                            @Override
//                            public void run() {
//                                Platform.runLater(() ->
//                                        Notification.create("اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ على نَبِيِّنَا مُحمَّد", null));
//                            }
//                        };
//                        timer.schedule(absoluteAzkarTask, 2000, 500000);


                        Runnable runnable = () -> Platform.runLater(() ->
                                Notification.create("اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ على نَبِيِّنَا مُحمَّد", null));

                        absoluteAzkarTask =
                                new EditablePeriodTimerTask(runnable, this::getPeriod);
                    }
                });
            }
        });
    }

    private Long getPeriod() {
        if (currentFrequency.equals(highFrequency)) {
            return 900000L;
        } else if (currentFrequency.equals(midFrequency)) {
            return 1800000L;
        } else if (currentFrequency.equals(lowFrequency)) {
            return 3600000L;
        } else if (currentFrequency.equals(rearFrequency)) {
            return 7200000L;
        }
        return 900000L;
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
        absoluteAzkarTask.updateTimer();
    }

    @FXML
    private void midFrequencyAction() {
        String msg = "ظهور كل" + " " + 30 + " " + "دقيقة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(midFrequency);
        absoluteAzkarTask.updateTimer();
    }

    @FXML
    private void lowFrequencyAction() {
        String msg = "ظهور كل" + " " + 1 + " " + "ساعة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(lowFrequency);
        absoluteAzkarTask.updateTimer();
    }


    @FXML
    private void rearFrequencyAction() {
        String msg = "ظهور كل" + " " + 2 + " " + "ساعة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(rearFrequency);
        absoluteAzkarTask.updateTimer();
    }

    private void toggleFrequencyBTN(JFXButton b) {
        currentFrequency.getStyleClass().remove("frequency-btn-selected");
        currentFrequency = b;
        currentFrequency.getStyleClass().add("frequency-btn-selected");
    }
}
