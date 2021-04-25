package com.bayoumi.controllers.prayertimes.info;

import com.bayoumi.models.City;
import com.bayoumi.models.Country;
import com.bayoumi.models.PrayerTimeSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Objects;

public class InfoController {
    @FXML
    private Label country; // TODO make it full name
    @FXML
    private Label city;
    @FXML
    private Label calcMethod;
    @FXML
    private Label asrJuristic;

    public void setData(PrayerTimeSettings prayerTimeSettings) {
        country.setText(Country.getCountryNameFormCode(prayerTimeSettings.getCountry()));
        city.setText(Objects.requireNonNull(City.getCityFromEngName(prayerTimeSettings.getCity(), prayerTimeSettings.getCountry())).getName());
        calcMethod.setText(prayerTimeSettings.getMethod().getArabicName());
        asrJuristic.setText(prayerTimeSettings.getAsrJuristic() == 0 ? "شافعي, مالكي, حنبلي" : "حنفي");
    }

}
