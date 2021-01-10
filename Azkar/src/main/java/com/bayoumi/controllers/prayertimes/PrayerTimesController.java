package com.bayoumi.controllers.prayertimes;

import com.bayoumi.Launcher;
import com.bayoumi.controllers.prayertimes.info.InfoController;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.Logger;
import com.bayoumi.util.WebService;
import com.jfoenix.controls.JFXDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PrayerTimesController implements Initializable {

    private PrayerTimes prayerTimes;
    @FXML
    private StackPane stackPane;
    @FXML
    private VBox infoBox;
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
            prayerTimes = WebService.getPrayerTimesToday();
            System.out.println("fetched: " + prayerTimes);

            Platform.runLater(() -> {
                if (prayerTimes.getPrayerTimeSettings().isSummerTiming()) {
                    prayerTimes.enableSummerTime();
                }

                OtherSettings otherSettings = new OtherSettings();
                if (!otherSettings.isEnable24Format()) {
                    prayerTimes.formatTime24To12( otherSettings.getLanguageLocal());
                }
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

    @FXML
    private void showInfo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/prayertimes/info/Info.fxml"));
            JFXDialog dialog = new JFXDialog(stackPane, loader.load(), JFXDialog.DialogTransition.TOP);
            InfoController infoController = loader.getController();
            infoController.setData(prayerTimes.getPrayerTimeSettings());
            dialog.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".showInfo()");
        }
    }
}
