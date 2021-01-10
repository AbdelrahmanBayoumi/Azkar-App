package com.bayoumi.controllers.settings.azkar;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseHandler;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class AzkarSettingsController implements Initializable {

    @FXML
    private Spinner<Integer> azkarPeriod;
    @FXML
    private JFXComboBox<String> azkarAlarmComboBox;
    @FXML
    private JFXComboBox<String> nightAzkarTimeComboBox;
    @FXML
    private JFXComboBox<String> morningAzkarTimeComboBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // init Spinner Values
        azkarPeriod.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 240, 0));
        initAudioFiles();
        // init morning and night azkar reminder
        nightAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));
        nightAzkarTimeComboBox.setValue("لا تذكير");
        morningAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));
        morningAzkarTimeComboBox.setValue("لا تذكير");

        loadSettings();

        // disable unsupported features
        azkarPeriod.setDisable(true);
        nightAzkarTimeComboBox.setDisable(true);
        morningAzkarTimeComboBox.setDisable(true);
    }

    private void initAudioFiles() {
        ObservableList<String> audioFiles = FXCollections.observableArrayList();
        audioFiles.add("بدون صوت");
        File folder = new File("jarFiles/audio");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    audioFiles.add(file.getName());
                }
            }
        }
        azkarAlarmComboBox.setItems(audioFiles);
    }

    @FXML
    private void save() {
        try {
            DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
            databaseHandler.stat = databaseHandler.con.prepareStatement("UPDATE azkar_settings set morning_reminder = ?, night_reminder = ?, audio_name = ?, azkar_period = ?");
            databaseHandler.stat.setString(1, morningAzkarTimeComboBox.getValue());
            databaseHandler.stat.setString(2, nightAzkarTimeComboBox.getValue());
            databaseHandler.stat.setString(3, azkarAlarmComboBox.getValue());
            databaseHandler.stat.setInt(4, azkarPeriod.getValue());
            databaseHandler.stat.executeUpdate();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
        close();
    }

    @FXML
    private void close() {
        ((Stage) azkarPeriod.getScene().getWindow()).close();
    }

    private void loadSettings() {
        try {
            ResultSet res = DatabaseHandler.getInstance().con.prepareStatement("SELECT * FROM azkar_settings").executeQuery();
            if (res.next()) {
                morningAzkarTimeComboBox.setValue(res.getString(1));
                nightAzkarTimeComboBox.setValue(res.getString(2));
                azkarAlarmComboBox.setValue(res.getString(3));
                azkarPeriod.getValueFactory().setValue(res.getInt(4));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }
}
