package com.bayoumi.controllers.prayertimes;

import com.batoulapps.adhan.PrayerTimes;
import com.bayoumi.Launcher;
import com.bayoumi.controllers.prayertimes.info.InfoController;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.models.PrayerTimeSettings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.prayertimes.local.PrayerTimesUtil;
import com.jfoenix.controls.JFXDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class PrayerTimesController implements Initializable {

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
    private PrayerTimeSettings prayerTimeSettings;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(() -> {
            try {
                prayerTimeSettings = new PrayerTimeSettings();
                PrayerTimes prayerTimesToday = PrayerTimesUtil.getPrayerTimesToday(prayerTimeSettings);
                Platform.runLater(() -> {
                    OtherSettings otherSettings = new OtherSettings();
                    SimpleDateFormat formatter;
                    if (otherSettings.isEnable24Format()) {
                        formatter = new SimpleDateFormat("HH:mm", new Locale(otherSettings.getLanguageLocal()));
                    } else {
                        formatter = new SimpleDateFormat("hh:mm a", new Locale(otherSettings.getLanguageLocal()));
                    }
                    formatter.setTimeZone(TimeZone.getDefault());

                    fajrTime.setText(formatter.format(prayerTimesToday.fajr));
                    sunriseTime.setText(formatter.format(prayerTimesToday.sunrise));
                    dhuhrTime.setText(formatter.format(prayerTimesToday.dhuhr));
                    asrTime.setText(formatter.format(prayerTimesToday.asr));
                    maghribTime.setText(formatter.format(prayerTimesToday.maghrib));
                    ishaTime.setText(formatter.format(prayerTimesToday.isha));

                    day.textProperty().bind(Launcher.homeController.day.textProperty());
                    hijriDate.textProperty().bind(Launcher.homeController.hijriDate.textProperty());
                    gregorianDate.textProperty().bind(Launcher.homeController.gregorianDate.textProperty());
                    time.textProperty().bind(Launcher.homeController.timeLabel.textProperty());

                    loadingBox.setVisible(false);
                });
            } catch (Exception ex) {
                Logger.error("ERROR in fetching prayertimes", ex, getClass().getName() + ".initialize()");
                Platform.runLater(() -> {
                    if (BuilderUI.showConfirmAlert(false, "حدث خطأ في مواقيت الصلاة..." + "\n" + "هل تريد الذهاب لإعدادات المدينة؟")) {
                        ((Stage) loadingBox.getScene().getWindow()).close();
                        Launcher.homeController.settingsBTN.fire();
                    } else {
                        ((Stage) loadingBox.getScene().getWindow()).close();
                    }
                });
            }
        }).start();
    }

    @FXML
    private void showInfo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/prayertimes/info/Info.fxml"));
            JFXDialog dialog = new JFXDialog(stackPane, loader.load(), JFXDialog.DialogTransition.TOP);
            InfoController infoController = loader.getController();
            infoController.setData(prayerTimeSettings);
            dialog.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".showInfo()");
        }
    }
}
