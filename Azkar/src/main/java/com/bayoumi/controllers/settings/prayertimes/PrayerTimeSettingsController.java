package com.bayoumi.controllers.settings.prayertimes;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseHandler;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PrayerTimeSettingsController implements Initializable {
    private PrayerTimes.PrayerTimeSettings prayerTimeSettings;
    @FXML
    private JFXTextField country;
    @FXML
    private JFXTextField city;
    @FXML
    private ComboBox<PrayerTimes.PrayerTimeSettings.Method> methodComboBox;
    @FXML
    private ToggleGroup asrJuristic;
    @FXML
    private JFXRadioButton hanafiRadioButton;
    @FXML
    private JFXRadioButton standardJuristic;
    @FXML
    private JFXCheckBox summerTiming;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prayerTimeSettings = new PrayerTimes.PrayerTimeSettings();
        country.setText(prayerTimeSettings.getCountry());
        city.setText(prayerTimeSettings.getCity());
        methodComboBox.setItems(FXCollections.observableArrayList(
                PrayerTimes.PrayerTimeSettings.Method.getListOfMethods()
        ));
        methodComboBox.setConverter(PrayerTimes.PrayerTimeSettings.Method.getStringConverter());
        methodComboBox.setValue(prayerTimeSettings.getMethod());

        if(prayerTimeSettings.getAsrJuristic() == 1){
            hanafiRadioButton.setSelected(true);
        }else{
            standardJuristic.setSelected(true);
        }

        summerTiming.setSelected(prayerTimeSettings.isSummerTiming());
    }

    @FXML
    private void save() {
        prayerTimeSettings.setCountry(country.getText());
        prayerTimeSettings.setCity(city.getText());
        prayerTimeSettings.setMethod(methodComboBox.getValue());
        prayerTimeSettings.setAsrJuristic(hanafiRadioButton.isSelected() ? 1 : 0);
        prayerTimeSettings.setSummerTiming(summerTiming.isSelected());
        prayerTimeSettings.save();
        close();
    }

    @FXML
    private void close() {
        ((Stage) country.getScene().getWindow()).close();
    }

}
