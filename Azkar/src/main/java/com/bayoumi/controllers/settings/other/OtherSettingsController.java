package com.bayoumi.controllers.settings.other;

import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.OtherSettings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import com.jfoenix.controls.JFXCheckBox;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class OtherSettingsController implements Initializable, SettingsInterface {
    private OtherSettings otherSettings;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private JFXCheckBox format24;
    @FXML
    private JFXCheckBox minimizeAtStart;
    @FXML
    private Spinner<Integer> hijriDateOffset;
    @FXML
    private JFXCheckBox darkTheme;
    @FXML
    private Label version;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        otherSettings = new OtherSettings();

        hijriDateOffset.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 10, 0));
        hijriDateOffset.getValueFactory().setValue(otherSettings.getHijriOffset());

        languageComboBox.setItems(FXCollections.observableArrayList("عربي - Arabic", "إنجليزي - English"));
        languageComboBox.setValue(otherSettings.getLanguage());
        languageComboBox.setDisable(true);

        format24.setSelected(otherSettings.isEnable24Format());

        minimizeAtStart.setSelected(otherSettings.isMinimized());

        darkTheme.setSelected(otherSettings.isEnableDarkMode());
        darkTheme.setDisable(true);

        version.setText(DatabaseManager.getInstance().getVersion());
    }

    @FXML
    private void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.abdelrahmanbayoumi.ml/Azkar-App/"));
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".openWebsite()");
        }
    }

    @Override
    public void saveToDB() {
        otherSettings.setLanguage(languageComboBox.getValue());
        otherSettings.setEnable24Format(format24.isSelected());
        otherSettings.setEnableDarkMode(darkTheme.isSelected());
        otherSettings.setHijriOffset(hijriDateOffset.getValue());
        otherSettings.setMinimized(minimizeAtStart.isSelected());
        otherSettings.save();
    }

}