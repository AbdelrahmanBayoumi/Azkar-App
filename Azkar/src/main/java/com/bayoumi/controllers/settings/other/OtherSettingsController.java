package com.bayoumi.controllers.settings.other;

import com.bayoumi.models.OtherSettings;
import com.jfoenix.controls.JFXCheckBox;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class OtherSettingsController implements Initializable {
    private OtherSettings otherSettings;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private JFXCheckBox format24;
    @FXML
    private JFXCheckBox darkTheme;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        otherSettings = new OtherSettings();

        languageComboBox.setItems(FXCollections.observableArrayList("عربي - Arabic", "إنجليزي - English"));
        languageComboBox.setValue(otherSettings.getLanguage());
        languageComboBox.setDisable(true);

        format24.setSelected(otherSettings.isEnable24Format());

        darkTheme.setSelected(otherSettings.isEnableDarkMode());
        darkTheme.setDisable(true);
    }

    @FXML
    private void close() {
        ((Stage) languageComboBox.getScene().getWindow()).close();
    }

    @FXML
    private void save() {
        otherSettings.setLanguage(languageComboBox.getValue());
        otherSettings.setEnable24Format(format24.isSelected());
        otherSettings.setEnableDarkMode(darkTheme.isSelected());
        otherSettings.save();
        close();
    }

}