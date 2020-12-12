package com.bayoumi.controllers.prayertimes;

import com.bayoumi.Launcher;
import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.WebService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PrayerTimesController implements Initializable {

    @FXML
    private Label day;
    @FXML
    private Label time;
    @FXML
    private Label hijriDate;
    @FXML
    private Label gregorianDate;
    @FXML
    private Label fajrText;
    @FXML
    private Label fajrTime;
    @FXML
    private Label sunriseText;
    @FXML
    private Label sunriseTime;
    @FXML
    private Label dhuhrText;
    @FXML
    private Label dhuhrTime;
    @FXML
    private Label asrText;
    @FXML
    private Label asrTime;
    @FXML
    private Label maghribText;
    @FXML
    private Label maghribTime;
    @FXML
    private Label ishaText;
    @FXML
    private Label ishaTime;
    @FXML
    private VBox loadingBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(() -> {
            String language = "ar";
//            PrayerTimes prayerTimes = WebService.getPrayerTimes("Alexandria", "Egypt", "5", HijriDate.getHijriDate(language));
            PrayerTimes prayerTimes = WebService.getPrayerTimesToday("Alexandria", "Egypt", "5");
            System.out.println("EE: " + prayerTimes);
            Platform.runLater(() -> {
                fajrTime.setText(prayerTimes.getFajr());
                sunriseTime.setText(prayerTimes.getSunrise());
                dhuhrTime.setText(prayerTimes.getDhuhr());
                asrTime.setText(prayerTimes.getAsr());
                maghribTime.setText(prayerTimes.getMaghrib());
                ishaTime.setText(prayerTimes.getIsha());

                day.textProperty().bind(Launcher.homeController.day.textProperty());
                hijriDate.textProperty().bind(Launcher.homeController.hijriDate.textProperty());
                gregorianDate.textProperty().bind(Launcher.homeController.gregorianDate.textProperty());
                time.textProperty().bind(Launcher.homeController.timeLabel.textProperty());
                loadingBox.setVisible(false);
            });
        }).start();
    }


}
