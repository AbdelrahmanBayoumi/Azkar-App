package com.bayoumi.controllers.home.prayertimes;

import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.SunnahTimes;
import com.bayoumi.Launcher;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
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
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class PrayerTimesController implements Initializable {

    // ==== Helper Objects ====
    private String currentPrayerValue;
    private Prayer currentPrayer;
    private ResourceBundle bundle;
    private SimpleDateFormat formatter;
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

        // -- Handle showing remaining time onHover for each prayer
        fajrTime.setOnMouseEntered(event -> showRemainingTime(fajrTime, Utilities.getCurrentDate(), prayerTimesToday.fajr));
        sunriseTime.setOnMouseEntered(event -> showRemainingTime(sunriseTime, Utilities.getCurrentDate(), prayerTimesToday.sunrise));
        dhuhrTime.setOnMouseEntered(event -> showRemainingTime(dhuhrTime, Utilities.getCurrentDate(), prayerTimesToday.dhuhr));
        asrTime.setOnMouseEntered(event -> showRemainingTime(asrTime, Utilities.getCurrentDate(), prayerTimesToday.asr));
        maghribTime.setOnMouseEntered(event -> showRemainingTime(maghribTime, Utilities.getCurrentDate(), prayerTimesToday.maghrib));
        ishaTime.setOnMouseEntered(event -> showRemainingTime(ishaTime, Utilities.getCurrentDate(), prayerTimesToday.isha));

        fajrTime.setOnMouseExited(event -> setPrayerTimeWithFormat(fajrTime, prayerTimesToday.fajr, formatter));
        sunriseTime.setOnMouseExited(event -> setPrayerTimeWithFormat(sunriseTime, prayerTimesToday.sunrise, formatter));
        dhuhrTime.setOnMouseExited(event -> setPrayerTimeWithFormat(dhuhrTime, prayerTimesToday.dhuhr, formatter));
        asrTime.setOnMouseExited(event -> setPrayerTimeWithFormat(asrTime, prayerTimesToday.asr, formatter));
        maghribTime.setOnMouseExited(event -> setPrayerTimeWithFormat(maghribTime, prayerTimesToday.maghrib, formatter));
        ishaTime.setOnMouseExited(event -> setPrayerTimeWithFormat(ishaTime, prayerTimesToday.isha, formatter));
    }

    private void showRemainingTime(Label label, Date dateNow, Date nextPrayerTime) {
        String remainingTime = "";
        if (dateNow.compareTo(nextPrayerTime) < 0) {
            // dateNow is before nextPrayerTime
            remainingTime = "- ";
        } else if (dateNow.compareTo(nextPrayerTime) > 0) {
            // dateNow is after nextPrayerTime
            remainingTime = "+ ";
        }
        remainingTime += Utilities.findDifference(dateNow, nextPrayerTime);
        label.setText(remainingTime);
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

    public void prayerTimelineAction(Date currentDate) {
        // get 'next Prayer' and if 'next Prayer'=NONE get 'currentPrayer'
        // to update the new current prayerBox
        final Prayer currentPrayerValue = prayerTimesToday.currentPrayer(currentDate);
        final Prayer nextPrayerValue = prayerTimesToday.nextPrayer(currentDate);
        if (nextPrayerValue.equals(Prayer.NONE)) {
            currentPrayer = prayerTimesToday.currentPrayer(currentDate);
        } else if (currentPrayerValue.equals(Prayer.NONE)) {
            currentPrayer = nextPrayerValue;
        } else {
            // if the differance between the abs of currentDate and currentPrayerDate is less than 35 min
            // then the currentPrayer is the currentPrayer else the currentPrayer is the nextPrayerValue
            final Date currentPrayerDate = prayerTimesToday.timeForPrayer(currentPrayerValue);
            if ((currentDate.getTime() - currentPrayerDate.getTime()) < 35 * 60 * 1000) {
                currentPrayer = currentPrayerValue;
            } else {
                currentPrayer = nextPrayerValue;
            }
        }

        switch (currentPrayer) {
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

    public Prayer getCurrentPrayer() {
        return currentPrayer;
    }

    @FXML
    private void openSunnahBox() {
        sunnahBox.setVisible(true);
    }

    @FXML
    private void removeSunnahBox() {
        sunnahBox.setVisible(false);
    }

    private void setPrayerTimeWithFormat(Label label, Date date, SimpleDateFormat formatter) {
        label.setText(formatter.format(date));
    }

    public void setPrayerTimesValuesToGUI() {
        Platform.runLater(() -> {
            if (settings.getOtherSettings().isEnable24Format()) {
                formatter = new SimpleDateFormat("HH:mm", new Locale(settings.getOtherSettings().getLanguageLocal()));
            } else {
                formatter = new SimpleDateFormat("hh:mm a", new Locale(settings.getOtherSettings().getLanguageLocal()));
            }
            formatter.setTimeZone(TimeZone.getDefault());

            setPrayerTimeWithFormat(fajrTime, prayerTimesToday.fajr, formatter);
            setPrayerTimeWithFormat(sunriseTime, prayerTimesToday.sunrise, formatter);
            setPrayerTimeWithFormat(dhuhrTime, prayerTimesToday.dhuhr, formatter);
            setPrayerTimeWithFormat(asrTime, prayerTimesToday.asr, formatter);
            setPrayerTimeWithFormat(maghribTime, prayerTimesToday.maghrib, formatter);
            setPrayerTimeWithFormat(ishaTime, prayerTimesToday.isha, formatter);

            SunnahTimes sunnahTimes = new SunnahTimes(prayerTimesToday);
            setPrayerTimeWithFormat(middleOfTheNightTime, sunnahTimes.middleOfTheNight, formatter);
            setPrayerTimeWithFormat(lastThirdOfTheNightTime, sunnahTimes.lastThirdOfTheNight, formatter);

            if (Utilities.isFriday(prayerTimesToday.dhuhr)) {
                dhuhrText.setText(Utility.toUTF(bundle.getString("jummah")));
            } else {
                dhuhrText.setText(Utility.toUTF(bundle.getString("dhuhr")));
            }
        });
    }

    @FXML
    private void reload() {
        Logger.debug("reload() start");
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
