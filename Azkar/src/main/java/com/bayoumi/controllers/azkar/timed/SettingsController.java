package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.controllers.settings.SettingsInterface;
import com.bayoumi.models.settings.AzkarSettings;
import com.bayoumi.models.settings.Settings;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class SettingsController implements SettingsInterface {

    private AzkarSettings azkarSettings;
    @FXML
    private JFXComboBox<String> nightAzkarTimeComboBox;
    @FXML
    private JFXComboBox<String> morningAzkarTimeComboBox;
    private ObservableList<Text> list;
    private JFXDialog dialog;

    public void setData(ObservableList<Text> list, JFXDialog dialog) {
        this.list = list;
        this.dialog = dialog;

        // init morning and night azkar reminder
        nightAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));
        morningAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));

        // init Saved data form DB
        azkarSettings = Settings.getInstance().getAzkarSettings();
        morningAzkarTimeComboBox.setValue(azkarSettings.getMorningAzkarReminder());
        nightAzkarTimeComboBox.setValue(azkarSettings.getNightAzkarReminder());
        dialog.setOnDialogClosed(event -> saveToDB());
    }


    @FXML
    private void maximizeFont() {
        for (Text text : list) {
            text.setFont(Font.font("System", FontWeight.BOLD, (text.getFont().getSize() + 1) > 60 ? 60 : text.getFont().getSize() + 1));
        }
    }

    @FXML
    private void minimizeFont() {
        for (Text text : list) {
            text.setFont(Font.font("System", FontWeight.BOLD, (text.getFont().getSize() - 1) < 15 ? 15 : text.getFont().getSize() - 1));
        }
    }

    @FXML
    private void keyEventAction(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ADD)) {
            maximizeFont();
        } else if (keyEvent.getCode().equals(KeyCode.SUBTRACT)) {
            minimizeFont();
        } else if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
            dialog.close();
        }
    }

    @Override
    public void saveToDB() {
        azkarSettings.setMorningAzkarReminder(morningAzkarTimeComboBox.getValue());
        azkarSettings.setNightAzkarReminder(nightAzkarTimeComboBox.getValue());
        azkarSettings.save();
    }
}
