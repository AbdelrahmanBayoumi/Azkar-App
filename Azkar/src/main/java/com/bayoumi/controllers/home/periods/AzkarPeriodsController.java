package com.bayoumi.controllers.home.periods;

import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.PopOverUtil;
import com.bayoumi.util.services.azkar.AzkarService;
import com.bayoumi.util.time.ArabicNumeralDiscrimination;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class AzkarPeriodsController implements Initializable {
    // ==== Objects ====
    private Settings settings;
    private ResourceBundle bundle;
    // =========
    @FXML
    private VBox periodBox;
    @FXML
    private Label frequencyLabel, title;
    @FXML
    private JFXButton highFrequency, midFrequency, lowFrequency, rearFrequency, currentFrequency;

    public void setData() {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        currentFrequency = getSelectedButton();
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        if (!settings.getAzkarSettings().isStopped()) {
            AzkarService.init(this);
        } else {
            periodBox.setDisable(true);
        }
        LanguageBundle.getInstance().addObserver((o, arg) -> {
            updateBundle(LanguageBundle.getInstance().getResourceBundle());
            setFrequencyLabel();
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settings = Settings.getInstance();
    }

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        PopOverUtil.init(frequencyLabel, Utility.toUTF(bundle.getString("azkar.period.tooltip")));
        title.setText(Utility.toUTF(bundle.getString("azkar.period.title")));
        highFrequency.setText(Utility.toUTF(bundle.getString("azkar.period.high")));
        midFrequency.setText(Utility.toUTF(bundle.getString("azkar.period.mid")));
        lowFrequency.setText(Utility.toUTF(bundle.getString("azkar.period.low")));
        rearFrequency.setText(Utility.toUTF(bundle.getString("azkar.period.rear")));
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
        String msg = Utility.toUTF(bundle.getString("azkar.period.showEvery")) + " ";
        if (currentFrequency.equals(highFrequency)) {
            if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(bundle, settings.getAzkarSettings().getHighPeriod());
            } else {
                msg += ArabicNumeralDiscrimination.getTimeGeneralPlurality(bundle, settings.getAzkarSettings().getHighPeriod());
            }
        } else if (currentFrequency.equals(midFrequency)) {
            if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(bundle, settings.getAzkarSettings().getMidPeriod());
            } else {
                msg += ArabicNumeralDiscrimination.getTimeGeneralPlurality(bundle, settings.getAzkarSettings().getMidPeriod());
            }
        } else if (currentFrequency.equals(lowFrequency)) {
            if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(bundle, settings.getAzkarSettings().getLowPeriod());
            } else {
                msg += ArabicNumeralDiscrimination.getTimeGeneralPlurality(bundle, settings.getAzkarSettings().getLowPeriod());
            }
        } else if (currentFrequency.equals(rearFrequency)) {
            if (Settings.getInstance().getLanguage().equals(Language.Arabic)) {
                msg += ArabicNumeralDiscrimination.getTimeArabicPlurality(bundle, settings.getAzkarSettings().getRearPeriod());
            } else {
                msg += ArabicNumeralDiscrimination.getTimeGeneralPlurality(bundle, settings.getAzkarSettings().getRearPeriod());
            }
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
