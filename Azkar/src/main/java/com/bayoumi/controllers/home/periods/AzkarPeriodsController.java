package com.bayoumi.controllers.home.periods;

import com.batoulapps.adhan.PrayerTimes;
import com.bayoumi.models.settings.*;
import com.bayoumi.util.services.azkar.AzkarService;
import com.bayoumi.util.time.ArabicNumeralDiscrimination;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.ResourceBundle;

public class AzkarPeriodsController implements Initializable{
    // ==== Objects ====
    private Settings settings;
    // =========
    @FXML
    private VBox periodBox;
    @FXML
    private Label frequencyLabel;
    @FXML
    private JFXButton highFrequency, midFrequency, lowFrequency, rearFrequency, currentFrequency;

    public void setData(Settings settings){
        currentFrequency = getSelectedButton();
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        if (!settings.getAzkarSettings().isStopped()) {
            AzkarService.init(this, settings.getNotificationSettings());
        } else {
            periodBox.setDisable(true);
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settings = Settings.getInstance();
        buildPopOver();
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

        AzkarService.updateTimer();
        setFrequencyLabel();
        settings.getAzkarSettings().setSelectedPeriod(currentFrequency.getText());
        settings.getAzkarSettings().saveSelectedPeriod();
    }

    private JFXButton getSelectedButton() {
        if (settings.getAzkarSettings().getSelectedPeriod().equals(highFrequency.getText())) {
            return highFrequency;
        } else if (settings.getAzkarSettings().getSelectedPeriod().equals(midFrequency.getText())) {
            return midFrequency;
        } else if (settings.getAzkarSettings().getSelectedPeriod().equals(lowFrequency.getText())) {
            return lowFrequency;
        } else if (settings.getAzkarSettings().getSelectedPeriod().equals(rearFrequency.getText())) {
            return rearFrequency;
        }
        return highFrequency;
    }

    public void setFrequencyLabel() {
        String msg = "ظهور كل" + " ";
        if (currentFrequency.equals(highFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(settings.getAzkarSettings().getHighPeriod());
        } else if (currentFrequency.equals(midFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(settings.getAzkarSettings().getMidPeriod());
        } else if (currentFrequency.equals(lowFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(settings.getAzkarSettings().getLowPeriod());
        } else if (currentFrequency.equals(rearFrequency)) {
            msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(settings.getAzkarSettings().getRearPeriod());
        }
        frequencyLabel.setText(msg);
    }

    public Long getPeriod() {
        if (currentFrequency.equals(highFrequency)) {
            return settings.getAzkarSettings().getHighPeriod() * 60000L;
        } else if (currentFrequency.equals(midFrequency)) {
            return settings.getAzkarSettings().getMidPeriod() * 60000L;
        } else if (currentFrequency.equals(lowFrequency)) {
            return settings.getAzkarSettings().getLowPeriod() * 60000L;
        } else if (currentFrequency.equals(rearFrequency)) {
            return settings.getAzkarSettings().getRearPeriod() * 60000L;
        }
        return 300000L;
//        if (currentFrequency.equals(highFrequency)) {
//            return 3000L;
//        } else if (currentFrequency.equals(midFrequency)) {
//            return 30000L;
//        } else if (currentFrequency.equals(lowFrequency)) {
//            return 40000L;
//        } else if (currentFrequency.equals(rearFrequency)) {
//            return 50000L;
//        }
//        return 50000L;
    }

}
