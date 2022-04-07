package com.bayoumi.controllers.components;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.ResourceBundle;

public class PrayerCalculationsController implements Initializable {

    @FXML
    private Label calculationMethodText, asrMadhabText, daylightSavingNote;
    @FXML
    public ComboBox<PrayerTimeSettings.Method> methodComboBox;
    @FXML
    public JFXRadioButton standardJuristic, hanafiRadioButton;
    @FXML
    private ToggleGroup asrJuristic;
    @FXML
    public JFXCheckBox summerTiming;

    public void updateBundle(ResourceBundle bundle) {
        calculationMethodText.setText(Utility.toUTF(bundle.getString("calculationMethod")));
        asrMadhabText.setText(Utility.toUTF(bundle.getString("asrMadhab")));
        daylightSavingNote.setText(Utility.toUTF(bundle.getString("daylightSavingNote")));
        summerTiming.setText(Utility.toUTF(bundle.getString("extraOneHourDayLightSaving")));
        standardJuristic.setText(Utility.toUTF(bundle.getString("asrMadhabJumhoor")));
        hanafiRadioButton.setText(Utility.toUTF(bundle.getString("hanafi")));
    }

    public void setData() {
        PrayerTimeSettings prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        methodComboBox.setValue(prayerTimeSettings.getMethod());
        // init Asr Juristic
        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }
        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());
        methodComboBox.setConverter(PrayerTimeSettings.Method.getStringConverter(Settings.getInstance().getOtherSettings().getLanguageLocal().equals("ar")));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        methodComboBox.setItems(FXCollections.observableArrayList(PrayerTimeSettings.Method.getListOfMethods()));
    }
}
