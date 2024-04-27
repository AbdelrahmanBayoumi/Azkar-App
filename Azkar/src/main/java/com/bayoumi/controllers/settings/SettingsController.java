package com.bayoumi.controllers.settings;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.load.Locations;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    private SettingsInterface settingsI;
    // ====== GUI Objects ======
    private JFXButton selectedButton;
    @FXML
    private JFXButton cityButton;
    @FXML
    private JFXButton azkarButton;
    @FXML
    private JFXButton otherButton;
    @FXML
    private BorderPane borderPane;


    public void updateBundle(ResourceBundle bundle) {
        cityButton.setText(Utility.toUTF(bundle.getString("city")));
        azkarButton.setText(Utility.toUTF(bundle.getString("azkar")));
        otherButton.setText(Utility.toUTF(bundle.getString("other")));
        borderPane.setNodeOrientation(NodeOrientation.valueOf(Utility.toUTF(bundle.getString("dir"))));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        LanguageBundle.getInstance().addObserver((o, arg) -> updateBundle(LanguageBundle.getInstance().getResourceBundle()));

        openAzkarSettings();
        borderPane.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
                        newWindow.setOnCloseRequest(event -> this.settingsI.saveToDB());
                    }
                });
            }
        });
    }

    @FXML
    private void openAzkarSettings() {
        try {
            if (!azkarButton.equals(selectedButton)) {
                toggle(azkarButton);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.AzkarSettings.toString()));
                borderPane.setCenter(loader.load());
                settingsI = loader.getController();
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".openAzkarSettings()");
        }
    }

    @FXML
    private void openCitySettings() {
        try {
            if (!cityButton.equals(selectedButton)) {
                toggle(cityButton);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.PrayerTimeSettings.toString()));
                borderPane.setCenter(loader.load());
                settingsI = loader.getController();
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".openCitySettings()");
        }
    }

    @FXML
    private void openOtherSettings() {
        try {
            if (!otherButton.equals(selectedButton)) {
                toggle(otherButton);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Locations.OtherSettings.toString()));
                borderPane.setCenter(loader.load());
                settingsI = loader.getController();
            }
        } catch (Exception ex) {
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

        if (settingsI != null) {
            settingsI.saveToDB();
        }
    }

}
