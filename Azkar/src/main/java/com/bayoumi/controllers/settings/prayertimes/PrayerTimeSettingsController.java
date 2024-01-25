package com.bayoumi.controllers.settings.prayertimes;

import com.bayoumi.controllers.components.PrayerCalculationsController;
import com.bayoumi.controllers.components.SelectLocationController;
import com.bayoumi.controllers.components.audio.ChooseAudioController;
import com.bayoumi.controllers.components.audio.ChooseAudioUtil;
import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.gui.load.Loader;
import com.bayoumi.util.gui.load.Locations;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class PrayerTimeSettingsController implements Initializable, SettingsInterface {
    private PrayerTimeSettings prayerTimeSettings;
    private ChooseAudioController chooseAudioController;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox container, adhanContainer;
    @FXML
    private Label adhanLabel;

    private ResourceBundle bundle;

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        adhanLabel.setText(Utility.toUTF(bundle.getString("adhan")));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();
        ScrollHandler.init(container, scrollPane, 4);
        chooseAudioController = ChooseAudioUtil.adhan(bundle, adhanContainer);
        try {
            container.getChildren().add(Loader.getInstance().getView(Locations.SelectLocation));
            ((SelectLocationController) Loader.getInstance().getController(Locations.SelectLocation)).setData();

            container.getChildren().add(Loader.getInstance().getView(Locations.PrayerCalculations));
            ((PrayerCalculationsController) Loader.getInstance().getController(Locations.PrayerCalculations)).setData();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".initialize()");
        }
    }


    @Override
    public void saveToDB() {
        try {
            final PrayerCalculationsController calculationsController = (PrayerCalculationsController) Loader.getInstance().getController(Locations.PrayerCalculations);
            final SelectLocationController selectLocationController = (SelectLocationController) Loader.getInstance().getController(Locations.SelectLocation);

            prayerTimeSettings.setCountry(selectLocationController.countries.getValue().getCode());
            prayerTimeSettings.setCity(selectLocationController.cities.getValue().getEnglishName());
            prayerTimeSettings.setMethod(calculationsController.methodComboBox.getValue());
            prayerTimeSettings.setAsrJuristic(calculationsController.hanafiRadioButton.isSelected() ? 1 : 0);
            prayerTimeSettings.setSummerTiming(calculationsController.summerTiming.isSelected());
            prayerTimeSettings.setLatitude(Double.parseDouble(selectLocationController.manualLatitude.getText()));
            prayerTimeSettings.setLongitude(Double.parseDouble(selectLocationController.manualLongitude.getText()));

            prayerTimeSettings.setAdhanAudio(chooseAudioController.getValue().getFileName());
            prayerTimeSettings.save();
            ChooseAudioController.stopIfPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
