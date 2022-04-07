package com.bayoumi.controllers.home.prayertimes;

import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.SunnahTimes;
import com.bayoumi.Launcher;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.bayoumi.util.prayertimes.PrayerTimesUtil;
import com.bayoumi.util.time.Utilities;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class PrayerTimesController implements Initializable {

    // ==== Helper Objects ====
    private String currentPrayerValue;
    private ResourceBundle bundle;
    // ==== Settings Objects ====
    private Settings settings;
    private PrayerTimes prayerTimesToday;
    // ==== GUI Objects ====
    @FXML
    private StackPane prayerTimesBox;
    @FXML
    private Label middleOfTheNightTime, lastThirdOfTheNightTime;
    @FXML
    private VBox sunnahBox, loadingBox;
    @FXML
    private HBox fajrBox, sunriseBox, dhuhrBox, asrBox, maghribBox, ishaBox, currentPrayerBox;
    @FXML
    private Label fajrText, sunriseText, dhuhrText, asrText, maghribText, ishaText, lastThirdOfTheNightTimeText, middleOfTheNightTimeText, otherTimings;
    @FXML
    private Label fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prayerTimesBox.setOnMouseExited(event -> {
            if (sunnahBox.isVisible()) {
                sunnahBox.setVisible(false);
            }
        });
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
    }

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        final String dir = Utility.toUTF(bundle.getString("dir"));
        if (dir.equals("LEFT_TO_RIGHT")) {
            middleOfTheNightTimeText.setStyle("-fx-font-size: 22");
            middleOfTheNightTime.setStyle("-fx-font-size: 22");
            lastThirdOfTheNightTime.setStyle("-fx-font-size: 22");
            lastThirdOfTheNightTimeText.setStyle("-fx-font-size: 22");
            prayerTimesBox.setMinWidth(355);
        } else {
            prayerTimesBox.setMinWidth(Region.USE_COMPUTED_SIZE);
        }
        prayerTimesBox.setNodeOrientation(NodeOrientation.valueOf(dir));
        fajrText.setText(Utility.toUTF(bundle.getString("fajr")));
        sunriseText.setText(Utility.toUTF(bundle.getString("sunrise")));
        dhuhrText.setText(Utility.toUTF(bundle.getString("dhuhr")));
        asrText.setText(Utility.toUTF(bundle.getString("asr")));
        maghribText.setText(Utility.toUTF(bundle.getString("maghrib")));
        ishaText.setText(Utility.toUTF(bundle.getString("isha")));
        otherTimings.setText(Utility.toUTF(bundle.getString("otherTimings")));
        lastThirdOfTheNightTimeText.setText(Utility.toUTF(bundle.getString("lastThirdOfTheNightTime")));
        middleOfTheNightTimeText.setText(Utility.toUTF(bundle.getString("middleOfTheNightTime")));
    }

    public void setData(Settings settings, PrayerTimes prayerTimesToday) {
        this.settings = settings;
        setPrayerTimes(prayerTimesToday);
        LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));
    }

    public void setPrayerTimes(PrayerTimes prayerTimesToday) {
        this.prayerTimesToday = prayerTimesToday;
        setPrayerTimesValuesToGUI();
    }

    public void prayerTimelineAction() {
        // get 'next Prayer' and if 'next Prayer'=NONE get 'currentPrayer'
        // to update the new current prayerBox
        switch (prayerTimesToday.nextPrayer().equals(Prayer.NONE) ? prayerTimesToday.currentPrayer() : prayerTimesToday.nextPrayer()) {
            case FAJR:
                changeCurrentPrayerBox(fajrBox, Utility.toUTF(bundle.getString("fajrPrayer")));
                break;
            case SUNRISE:
                changeCurrentPrayerBox(sunriseBox, Utility.toUTF(bundle.getString("sunriseTime")));
                break;
            case DHUHR:
                changeCurrentPrayerBox(dhuhrBox, Utility.toUTF(bundle.getString("dhuhrPrayer")));
                break;
            case ASR:
                changeCurrentPrayerBox(asrBox, Utility.toUTF(bundle.getString("asrPrayer")));
                break;
            case MAGHRIB:
                changeCurrentPrayerBox(maghribBox, Utility.toUTF(bundle.getString("maghribPrayer")));
                break;
            case ISHA:
                changeCurrentPrayerBox(ishaBox, Utility.toUTF(bundle.getString("ishaPrayer")));
                break;
        }
        if (currentPrayerBox != null && !currentPrayerBox.getStyleClass().contains("box-prayer-selected")) {
            currentPrayerBox.getStyleClass().add("box-prayer-selected");
        }
    }

    private void changeCurrentPrayerBox(HBox box, String value) {
        if (currentPrayerBox != null && !currentPrayerBox.equals(box)) {
            currentPrayerBox.getStyleClass().remove("box-prayer-selected");
        }
        currentPrayerBox = box;
        currentPrayerValue = value;
    }

    public String getCurrentPrayerValue() {
        return currentPrayerValue;
    }

    @FXML
    private void openSunnahBox() {
        sunnahBox.setVisible(true);
    }

    @FXML
    private void removeSunnahBox() {
        sunnahBox.setVisible(false);
    }


    public void setPrayerTimesValuesToGUI() {
        Platform.runLater(() -> {
            SimpleDateFormat formatter;
            if (settings.getOtherSettings().isEnable24Format()) {
                formatter = new SimpleDateFormat("HH:mm", new Locale(settings.getOtherSettings().getLanguageLocal()));
            } else {
                formatter = new SimpleDateFormat("hh:mm a", new Locale(settings.getOtherSettings().getLanguageLocal()));
            }
            formatter.setTimeZone(TimeZone.getDefault());

            fajrTime.setText(formatter.format(prayerTimesToday.fajr));
            sunriseTime.setText(formatter.format(prayerTimesToday.sunrise));
            dhuhrTime.setText(formatter.format(prayerTimesToday.dhuhr));
            asrTime.setText(formatter.format(prayerTimesToday.asr));
            maghribTime.setText(formatter.format(prayerTimesToday.maghrib));
            ishaTime.setText(formatter.format(prayerTimesToday.isha));

            SunnahTimes sunnahTimes = new SunnahTimes(prayerTimesToday);
            middleOfTheNightTime.setText(formatter.format(sunnahTimes.middleOfTheNight));
            lastThirdOfTheNightTime.setText(formatter.format(sunnahTimes.lastThirdOfTheNight));


            if (Utilities.isFriday(prayerTimesToday.dhuhr)) {
                dhuhrText.setText(Utility.toUTF(bundle.getString("jummah")));
            } else {
                dhuhrText.setText(Utility.toUTF(bundle.getString("dhuhr")));
            }
        });
    }

    @FXML
    private void reload() {
        System.out.println("reload() start");
        new Thread(() -> {
            try {
                Platform.runLater(() -> loadingBox.setVisible(true));
                setPrayerTimes(PrayerTimesUtil.getPrayerTimesToday(settings.getPrayerTimeSettings(), Launcher.homeController.date));
                Thread.sleep(200);
                Platform.runLater(() -> loadingBox.setVisible(false));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
