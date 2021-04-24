package com.bayoumi.controllers.prayertimes.info;

import com.bayoumi.models.PrayerTimeSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class InfoController {
    @FXML
    private Label country;
    @FXML
    private Label city;
    @FXML
    private Label calcMethod;
    @FXML
    private Label asrJuristic;

    public void setData(PrayerTimeSettings prayerTimeSettings) {
        country.setText(prayerTimeSettings.getCountry());
        city.setText(prayerTimeSettings.getCity());
        calcMethod.setText(prayerTimeSettings.getMethod().getArabicName());
        asrJuristic.setText(prayerTimeSettings.getAsrJuristic() == 0 ? "شافعي, مالكي, حنبلي" : "حنفي");
    }

}
