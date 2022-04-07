package com.bayoumi.controllers.onboarding;

import com.bayoumi.controllers.components.PrayerCalculationsController;
import com.bayoumi.controllers.components.SelectLocationController;
import com.bayoumi.controllers.components.audio.ChooseAudioController;
import com.bayoumi.controllers.components.audio.ChooseAudioUtil;
import com.bayoumi.models.Onboarding;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.OtherSettings;
import com.bayoumi.models.settings.PrayerTimeSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ScrollHandler;
import com.bayoumi.util.gui.load.Loader;
import com.bayoumi.util.gui.load.Locations;
import com.jfoenix.controls.JFXCheckBox;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class OnboardingController implements Initializable {
    private ResourceBundle bundle;
    private ChooseAudioController chooseAudioController;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox container, adhanContainer;
    @FXML
    private JFXCheckBox format24;
    @FXML
    private Button saveAndFinish;
    @FXML
    private Label configureTheProgramSettings, adhanLabel, settingsCanBeChangedFromWithinTheProgramAsWell;
    @FXML
    private JFXCheckBox minimizeAtStart;
    private PrayerTimeSettings prayerTimeSettings;

    public void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        adhanLabel.setText(Utility.toUTF(bundle.getString("adhan")));
        format24.setText(Utility.toUTF(bundle.getString("hour24System")));
        minimizeAtStart.setText(Utility.toUTF(bundle.getString("minimizeAtStart")));
        saveAndFinish.setText(Utility.toUTF(bundle.getString("saveAndFinish")));
        configureTheProgramSettings.setText(Utility.toUTF(bundle.getString("configureTheProgramSettings")));
        settingsCanBeChangedFromWithinTheProgramAsWell.setText(Utility.toUTF(bundle.getString("settingsCanBeChangedFromWithinTheProgramAsWell")));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            updateBundle(LanguageBundle.getInstance().getResourceBundle());
            prayerTimeSettings = Settings.getInstance().getPrayerTimeSettings();


            chooseAudioController = ChooseAudioUtil.adhan(bundle, adhanContainer);
            if (chooseAudioController != null) {
                chooseAudioController.initFromFirstValue();
            }

            ScrollHandler.init(container, scrollPane, 4);

            container.getChildren().add(2, Loader.getInstance().getView(Locations.SelectLocation));
            ((SelectLocationController) Loader.getInstance().getController(Locations.SelectLocation)).setData();

            container.getChildren().add(3, Loader.getInstance().getView(Locations.PrayerCalculations));
            ((PrayerCalculationsController) Loader.getInstance().getController(Locations.PrayerCalculations)).setData();

            ((SelectLocationController) Loader.getInstance().getController(Locations.SelectLocation)).autoLocationButton.fire();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".initialize()");
        }
    }

    @FXML
    private void finish() throws Exception {
        // save prayerTimes settings
        final PrayerCalculationsController calculationsController = (PrayerCalculationsController) Loader.getInstance().getController(Locations.PrayerCalculations);
        final SelectLocationController selectLocationController = (SelectLocationController) Loader.getInstance().getController(Locations.SelectLocation);

        prayerTimeSettings.setCountry(selectLocationController.countries.getValue().getCode());
        prayerTimeSettings.setCity(selectLocationController.cities.getValue().getEnglishName());
        prayerTimeSettings.setMethod(calculationsController.methodComboBox.getValue());
        prayerTimeSettings.setAsrJuristic(calculationsController.hanafiRadioButton.isSelected() ? 1 : 0);
        prayerTimeSettings.setSummerTiming(calculationsController.summerTiming.isSelected());
        prayerTimeSettings.setLatitude(selectLocationController.cities.getValue().getLatitude());
        prayerTimeSettings.setLongitude(selectLocationController.cities.getValue().getLongitude());
        prayerTimeSettings.setAdhanAudio(chooseAudioController.getValue());
        ChooseAudioController.stopIfPlaying();
        prayerTimeSettings.save();
        // save other settings
        OtherSettings otherSettings = Settings.getInstance().getOtherSettings();
        otherSettings.setEnable24Format(format24.isSelected());
        otherSettings.setMinimized(minimizeAtStart.isSelected());
        otherSettings.save();

        Onboarding.setFirstTimeOpened(0);

        ((Stage) format24.getScene().getWindow()).close();
    }

}
