package com.bayoumi.controllers.settings;

import com.bayoumi.util.Logger;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    private JFXButton selectedButton;
    @FXML
    private JFXButton cityButton;
    @FXML
    private JFXButton azkarButton;
    @FXML
    private JFXButton otherButton;
    @FXML
    private BorderPane borderPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openCitySettings();
    }

    @FXML
    private void openAzkarSettings() {
        try {
            if (!azkarButton.equals(selectedButton)) {
                toggle(azkarButton);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/settings/azkar/AzkarSettings.fxml"));
                borderPane.setCenter(loader.load());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".openAzkarSettings()");
        }
    }

    @FXML
    private void openCitySettings() {
        try {
            if (!cityButton.equals(selectedButton)) {
                toggle(cityButton);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/settings/prayertimes/PrayerTimeSettings.fxml"));
                borderPane.setCenter(loader.load());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".openCitySettings()");
        }
    }

    @FXML
    private void openOtherSettings() {
        try {
            if (!otherButton.equals(selectedButton)) {
                toggle(otherButton);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/views/settings/other/OtherSettings.fxml"));
                borderPane.setCenter(loader.load());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".openOtherSettings()");
        }
    }

    private void toggle(JFXButton b) {
        // remove focus
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("primary-button");
            selectedButton.getStyleClass().add("secondary-button");
        }
        // add focus
        selectedButton = b;
        selectedButton.getStyleClass().remove("secondary-button");
        selectedButton.getStyleClass().add("primary-button");
    }


}
