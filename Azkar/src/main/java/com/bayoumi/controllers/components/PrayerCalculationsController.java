package com.bayoumi.controllers.components;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.bayoumi.util.time.ArabicNumeralDiscrimination;
import com.bayoumi.util.time.Utilities;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class PrayerCalculationsController implements Initializable {
    private ResourceBundle bundle;
    private PrayerTimeSettings prayerTimeSettings;
    @FXML
    public ComboBox<PrayerTimeSettings.Method> methodComboBox;
    @FXML
    public JFXRadioButton standardJuristic, hanafiRadioButton;
    @FXML
    private ToggleGroup asrJuristic;
    @FXML
    public JFXCheckBox summerTiming;
    @FXML
    private Spinner<Integer> fajrAdjustment, dhuhrAdjustment, sunriseAdjustment, asrAdjustment, maghribAdjustment, ishaAdjustment;
    @FXML
    private Label calculationMethodText, asrMadhabText, daylightSavingNote,
            prayerAdjustment, fajrText, sunriseText, dhuhrText, asrText, maghribText, ishaText,
            minPluralityFajr, minPluralitySunrise, minPluralityDhuhr, minPluralityAsr, minPluralityMaghrib, minPluralityIsha;

    private void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        calculationMethodText.setText(Utility.toUTF(bundle.getString("calculationMethod")));
        asrMadhabText.setText(Utility.toUTF(bundle.getString("asrMadhab")));
        daylightSavingNote.setText(Utility.toUTF(bundle.getString("daylightSavingNote")));
        summerTiming.setText(Utility.toUTF(bundle.getString("extraOneHourDayLightSaving")));
        standardJuristic.setText(Utility.toUTF(bundle.getString("asrMadhabJumhoor")));
        hanafiRadioButton.setText(Utility.toUTF(bundle.getString("hanafi")));

        minPluralityFajr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, prayerTimeSettings.getFajrAdjustment()));
        minPluralitySunrise.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, prayerTimeSettings.getSunriseAdjustment()));
        minPluralityDhuhr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, prayerTimeSettings.getDhuhrAdjustment()));
        minPluralityAsr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, prayerTimeSettings.getAsrAdjustment()));
        minPluralityMaghrib.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, prayerTimeSettings.getMaghribAdjustment()));
        minPluralityIsha.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, prayerTimeSettings.getIshaAdjustment()));

        prayerAdjustment.setText(Utility.toUTF(bundle.getString("prayerAdjutment")));
        fajrText.setText(Utility.toUTF(bundle.getString("fajr")));
        sunriseText.setText(Utility.toUTF(bundle.getString("sunrise")));
        if (Utilities.isFriday()) {
            dhuhrText.setText(Utility.toUTF(bundle.getString("jummah")));
        } else {
            dhuhrText.setText(Utility.toUTF(bundle.getString("dhuhr")));
        }
        asrText.setText(Utility.toUTF(bundle.getString("asr")));
        maghribText.setText(Utility.toUTF(bundle.getString("maghrib")));
        ishaText.setText(Utility.toUTF(bundle.getString("isha")));

        methodComboBox.getItems().setAll(PrayerTimeSettings.Method.getListOfMethods());
        methodComboBox.setValue(prayerTimeSettings.getMethod());
    }

    public void setData() {
        prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        // init Asr Juristic
        if (prayerTimeSettings.getAsrJuristic() == 1) {
            hanafiRadioButton.setSelected(true);
        } else {
            standardJuristic.setSelected(true);
        }
        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());


        fajrAdjustment.getValueFactory().setValue(prayerTimeSettings.getFajrAdjustment());
        dhuhrAdjustment.getValueFactory().setValue(prayerTimeSettings.getDhuhrAdjustment());
        sunriseAdjustment.getValueFactory().setValue(prayerTimeSettings.getSunriseAdjustment());
        asrAdjustment.getValueFactory().setValue(prayerTimeSettings.getAsrAdjustment());
        maghribAdjustment.getValueFactory().setValue(prayerTimeSettings.getMaghribAdjustment());
        ishaAdjustment.getValueFactory().setValue(prayerTimeSettings.getIshaAdjustment());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));

        methodComboBox.setConverter(PrayerTimeSettings.Method.getStringConverter());
        methodComboBox.setItems(FXCollections.observableArrayList(PrayerTimeSettings.Method.getListOfMethods()));

        fajrAdjustment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-120, 120, 0));
        dhuhrAdjustment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-120, 120, 0));
        sunriseAdjustment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-120, 120, 0));
        asrAdjustment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-120, 120, 0));
        maghribAdjustment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-120, 120, 0));
        ishaAdjustment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-120, 120, 0));
    }

    @FXML
    private void onFajrAdjustmentChange() {
        prayerTimeSettings.setFajrAdjustment(fajrAdjustment.getValue());
        prayerTimeSettings.handleNotifyObservers();
        minPluralityFajr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, fajrAdjustment.getValue()));
    }

    @FXML
    private void onDhuhrAdjustmentChange() {
        prayerTimeSettings.setDhuhrAdjustment(dhuhrAdjustment.getValue());
        prayerTimeSettings.handleNotifyObservers();
        minPluralityDhuhr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, dhuhrAdjustment.getValue()));
    }


    @FXML
    private void onSunriseAdjustmentChange() {
        prayerTimeSettings.setSunriseAdjustment(sunriseAdjustment.getValue());
        prayerTimeSettings.handleNotifyObservers();
        minPluralitySunrise.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, sunriseAdjustment.getValue()));
    }

    @FXML
    private void onAsrAdjustmentChange() {
        prayerTimeSettings.setAsrAdjustment(asrAdjustment.getValue());
        prayerTimeSettings.handleNotifyObservers();
        minPluralityAsr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, asrAdjustment.getValue()));
    }

    @FXML
    private void onMaghribAdjustmentChange() {
        prayerTimeSettings.setMaghribAdjustment(maghribAdjustment.getValue());
        prayerTimeSettings.handleNotifyObservers();
        minPluralityMaghrib.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, maghribAdjustment.getValue()));
    }

    @FXML
    private void onIshaAdjustmentChange() {
        prayerTimeSettings.setIshaAdjustment(ishaAdjustment.getValue());
        prayerTimeSettings.handleNotifyObservers();
        minPluralityIsha.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle, ishaAdjustment.getValue()));
    }

    @FXML
    private void onMethodSelect() {
        if (methodComboBox.getValue() != null) {
            prayerTimeSettings.setMethod(methodComboBox.getValue());
            prayerTimeSettings.handleNotifyObservers();
        }
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
