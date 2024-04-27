package com.bayoumi.controllers.components;

import com.bayoumi.models.settings.Language;
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

    private PrayerTimeSettings prayerTimeSettings;
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

    private void updateBundle(ResourceBundle bundle) {
        calculationMethodText.setText(Utility.toUTF(bundle.getString("calculationMethod")));
        asrMadhabText.setText(Utility.toUTF(bundle.getString("asrMadhab")));
        daylightSavingNote.setText(Utility.toUTF(bundle.getString("daylightSavingNote")));
        summerTiming.setText(Utility.toUTF(bundle.getString("extraOneHourDayLightSaving")));
        standardJuristic.setText(Utility.toUTF(bundle.getString("asrMadhabJumhoor")));
        hanafiRadioButton.setText(Utility.toUTF(bundle.getString("hanafi")));
        methodComboBox.setConverter(PrayerTimeSettings.Method.getStringConverter(Settings.getInstance().getLanguage().equals(Language.Arabic)));
        methodComboBox.setValue(prayerTimeSettings.getMethod());
    }

    public void setData() {
        prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        methodComboBox.setValue(prayerTimeSettings.getMethod());
        // init Asr Juristic
        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }
        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        methodComboBox.setItems(FXCollections.observableArrayList(PrayerTimeSettings.Method.getListOfMethods()));
        LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));
    }

    @FXML
    private void onMethodSelect() {
        prayerTimeSettings.setMethod(methodComboBox.getValue());
        prayerTimeSettings.handleNotifyObservers();
    }

    @FXML
    private void onAsrJuristicSelect() {
        prayerTimeSettings.setAsrJuristic(standardJuristic.isSelected() ? 0 : 1);
        prayerTimeSettings.handleNotifyObservers();
    }

    @FXML
    private void onSummerTimingSelect() {
        prayerTimeSettings.setSummerTiming(summerTiming.isSelected());
        prayerTimeSettings.handleNotifyObservers();
    }
}
