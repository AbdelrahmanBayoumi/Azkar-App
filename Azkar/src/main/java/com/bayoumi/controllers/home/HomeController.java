package com.bayoumi.controllers.home;

import com.bayoumi.Launcher;
import com.bayoumi.controllers.azkar.timed.TimedAzkarController;
import com.bayoumi.models.AbsoluteZekr;
import com.bayoumi.models.AzkarSettings;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.models.PrayerTimeSettings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.HelperMethods;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    public Label hijriDate;
    @FXML
    public Label day;
    @FXML
    public Label gregorianDate;
    @FXML
    public Label timeLabel;
    @FXML
    public JFXButton settingsBTN;
    private OtherSettings otherSettings;
    private AzkarSettings azkarSettings;
    @FXML
    private VBox periodBox;
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
        buildPopOver();

        otherSettings = new OtherSettings();
        azkarSettings = new AzkarSettings();

        currentFrequency = getSelectedButton();
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        if (!AbsoluteZekr.fetchData()) {
            Launcher.workFine.setValue(false);
            return;
        }

        if (!azkarSettings.isStopped()) {
            AzkarService.init(this);
        } else {
            periodBox.setDisable(true);
        }

        initClock();
        ReminderService.init(azkarSettings);
        Date date = new Date();
        day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
        gregorianDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
        hijriDate.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));
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
        /*
        if (currentFrequency.equals(highFrequency)) {
            return 1000L;
        } else if (currentFrequency.equals(midFrequency)) {
            return 30000L;
        } else if (currentFrequency.equals(lowFrequency)) {
            return 40000L;
        } else if (currentFrequency.equals(rearFrequency)) {
            return 50000L;
        }
        return 50000L;
        */
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

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            Date date = new Date();
            String timeNow;
            // check if there is a change in Hijri date offset
            if (OtherSettings.isUpdated) {
                otherSettings = new OtherSettings();
                OtherSettings.isUpdated = false;
                hijriDate.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));
            }
            if (otherSettings.isEnable24Format()) {
                timeNow = Utilities.getTime24(otherSettings.getLanguageLocal(), date);
            } else {
                timeNow = Utilities.getTime(otherSettings.getLanguageLocal(), date);
            }
            timeLabel.setText(timeNow);

            // Is it new day? => change Dates and the prayer times
            if (timeNow.equals("12:00:00 AM") || timeNow.equals("12:00:00 ص") || timeNow.equals("00:00:00")) {
                day.setText(Utilities.getDay(otherSettings.getLanguageLocal(), date));
                gregorianDate.setText(Utilities.getGregorianDate(otherSettings.getLanguageLocal(), date));
                hijriDate.setText(new HijriDate(otherSettings.getHijriOffset()).getString(otherSettings.getLanguageLocal()));
            }

            // check if time periods of Azkar and settings has changed
            if (AzkarSettings.isUpdated) {
                azkarSettings = new AzkarSettings();
                AzkarSettings.isUpdated = false;
                if (AzkarService.absoluteAzkarTask != null) {
                    AzkarService.absoluteAzkarTask.stopTask();
                }
                if (azkarSettings.isStopped()) {
                    periodBox.setDisable(true);
                } else {
                    AzkarService.init(this);
                    periodBox.setDisable(false);
                }
                ReminderService.init(azkarSettings);
            }
            if (PrayerTimeSettings.isUpdated) {
                ReminderService.init(azkarSettings);
                PrayerTimeSettings.isUpdated = false;
            }
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

    @FXML
    private void goToPrayerTimes() {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/views/prayertimes/PrayerTimes.fxml"))));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            HelperMethods.ExitKeyCodeCombination(stage.getScene(), stage);
            stage.show();
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
