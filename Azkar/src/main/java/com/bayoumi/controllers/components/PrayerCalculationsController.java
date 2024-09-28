package com.bayoumi.controllers.components;

import com.bayoumi.models.settings.Language;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.IntegerSpinner;
import com.bayoumi.util.time.ArabicNumeralDiscrimination;
import com.bayoumi.util.time.Utilities;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Calendar;
import java.util.ResourceBundle;

public class PrayerCalculationsController implements Initializable {
    private ResourceBundle bundle;
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
    @FXML
    private Spinner<Integer> fajrAdjusment, dhuhrAdjusment, sunriseAdjusment, asrAdjusment,maghribAdjusment,ishaAdjusment;
    @FXML
    public Label prayerAdjutment;
    @FXML
    public Label fajrText,sunriseText,dhuhrText,asrText,maghribText,ishaText;
    @FXML
    public Label minPluralityFajr,minPluralitySunrise,minPluralityDhuhr,minPluralityAsr,minPluralityMaghrib,minPluralityIsha;

    private void updateBundle(ResourceBundle bundle) {
        this.bundle=bundle;
        calculationMethodText.setText(Utility.toUTF(bundle.getString("calculationMethod")));
        asrMadhabText.setText(Utility.toUTF(bundle.getString("asrMadhab")));
        daylightSavingNote.setText(Utility.toUTF(bundle.getString("daylightSavingNote")));
        summerTiming.setText(Utility.toUTF(bundle.getString("extraOneHourDayLightSaving")));
        standardJuristic.setText(Utility.toUTF(bundle.getString("asrMadhabJumhoor")));
        hanafiRadioButton.setText(Utility.toUTF(bundle.getString("hanafi")));
        minPluralityFajr.setText(Utility.toUTF(bundle.getString("oneMinute")));
        minPluralitySunrise.setText(Utility.toUTF(bundle.getString("oneMinute")));
        minPluralityDhuhr.setText(Utility.toUTF(bundle.getString("oneMinute")));
        minPluralityAsr.setText(Utility.toUTF(bundle.getString("oneMinute")));
        minPluralityMaghrib.setText(Utility.toUTF(bundle.getString("oneMinute")));
        minPluralityIsha.setText(Utility.toUTF(bundle.getString("oneMinute")));

        prayerAdjutment.setText(Utility.toUTF(bundle.getString("prayerAdjutment")));
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

        fajrAdjusment.getValueFactory().setValue(prayerTimeSettings.getFajrAdjusment());
        dhuhrAdjusment.getValueFactory().setValue(prayerTimeSettings.getDhuhrAdjusment());
        sunriseAdjusment.getValueFactory().setValue(prayerTimeSettings.getSunriseAdjusment());
        asrAdjusment.getValueFactory().setValue(prayerTimeSettings.getAsrAdjusment());
        maghribAdjusment.getValueFactory().setValue(prayerTimeSettings.getMaghribAdjusment());
        ishaAdjusment.getValueFactory().setValue(prayerTimeSettings.getIshaAdjusment());

        updateBundle(LanguageBundle.getInstance().getResourceBundle());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        methodComboBox.setItems(FXCollections.observableArrayList(PrayerTimeSettings.Method.getListOfMethods()));
        LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));
        IntegerSpinner.init(fajrAdjusment);
        IntegerSpinner.init(sunriseAdjusment);
        IntegerSpinner.init(dhuhrAdjusment);
        IntegerSpinner.init(asrAdjusment);
        IntegerSpinner.init(maghribAdjusment);
        IntegerSpinner.init(ishaAdjusment);

        fajrAdjusment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        fajrAdjusment.editableProperty().setValue(false);
        fajrAdjusment.valueProperty().addListener((observable, oldValue, newValue) ->{
            minPluralityFajr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle,newValue));
            if(!newValue.equals(oldValue)){
                prayerTimeSettings.setFajrAdjusment(newValue);
                prayerTimeSettings.handleNotifyObservers();
            }

        });


        dhuhrAdjusment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        dhuhrAdjusment.editableProperty().setValue(false);
        dhuhrAdjusment.valueProperty().addListener((observable, oldValue, newValue) ->{
            minPluralityDhuhr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle,newValue));
            if(!newValue.equals(oldValue)){
                prayerTimeSettings.setDhuhrAdjusment(newValue);
                prayerTimeSettings.handleNotifyObservers();
            }

        });

        sunriseAdjusment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        sunriseAdjusment.editableProperty().setValue(false);
        sunriseAdjusment.valueProperty().addListener((observable, oldValue, newValue) ->{
            minPluralitySunrise.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle,newValue));
            if(!newValue.equals(oldValue)){
                prayerTimeSettings.setSunriseAdjusment(newValue);
                prayerTimeSettings.handleNotifyObservers();
            }

        });

        asrAdjusment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        asrAdjusment.editableProperty().setValue(false);
        asrAdjusment.valueProperty().addListener((observable, oldValue, newValue) ->{
            minPluralityAsr.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle,newValue));
            if(!newValue.equals(oldValue)){
                prayerTimeSettings.setAsrAdjusment(newValue);
                prayerTimeSettings.handleNotifyObservers();
            }

        });

        maghribAdjusment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        maghribAdjusment.editableProperty().setValue(false);
        maghribAdjusment.valueProperty().addListener((observable, oldValue, newValue) ->{
            minPluralityMaghrib.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle,newValue));
            if(!newValue.equals(oldValue)){
                prayerTimeSettings.setMaghribAdjusment(newValue);
                prayerTimeSettings.handleNotifyObservers();
            }

        });

        ishaAdjusment.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        ishaAdjusment.editableProperty().setValue(false);
        ishaAdjusment.valueProperty().addListener((observable, oldValue, newValue) ->{
            minPluralityIsha.setText(ArabicNumeralDiscrimination.minutesArabicPlurality(bundle,newValue));
            if(!newValue.equals(oldValue)){
                prayerTimeSettings.setIshaAdjusment(newValue);
                prayerTimeSettings.handleNotifyObservers();
            }

        });

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
