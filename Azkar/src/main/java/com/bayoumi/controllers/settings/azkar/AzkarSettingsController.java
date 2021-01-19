package com.bayoumi.controllers.settings.azkar;

import com.bayoumi.models.AzkarSettings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.validation.SingleInstance;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AzkarSettingsController implements Initializable {

    private AzkarSettings azkarSettings;
    private JFXButton currentFrequency;
    @FXML
    private VBox periodBox;
    @FXML
    private JFXButton highFrequency;
    @FXML
    private JFXButton midFrequency;
    @FXML
    private JFXButton lowFrequency;
    @FXML
    private JFXButton rearFrequency;
    @FXML
    private Spinner<Integer> azkarPeriod;
    @FXML
    private JFXCheckBox stopAzkar;
    @FXML
    private JFXComboBox<String> azkarAlarmComboBox;
    @FXML
    private JFXComboBox<String> nightAzkarTimeComboBox;
    @FXML
    private JFXComboBox<String> morningAzkarTimeComboBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        periodBox.disableProperty().bind(stopAzkar.selectedProperty());
        // init Spinner Values
        azkarPeriod.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 240, 1));

        initAudioFiles();
        // init morning and night azkar reminder
        nightAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));
        morningAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));

        // init Saved data form DB
        azkarSettings = new AzkarSettings();
        morningAzkarTimeComboBox.setValue(azkarSettings.getMorningAzkarReminder());
        nightAzkarTimeComboBox.setValue(azkarSettings.getNightAzkarReminder());
        azkarAlarmComboBox.setValue(azkarSettings.getAudioName());
        azkarPeriod.getValueFactory().setValue(azkarSettings.getHighPeriod());
        stopAzkar.setSelected(azkarSettings.isStopped());
        currentFrequency = highFrequency;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        // disable unsupported features
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
    private void highFrequencyAction() {
        toggleFrequencyBTN(highFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getHighPeriod());
    }

    @FXML
    private void lowFrequencyAction() {
        toggleFrequencyBTN(lowFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getLowPeriod());
    }

    @FXML
    private void midFrequencyAction() {
        toggleFrequencyBTN(midFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getMidPeriod());
    }

    @FXML
    private void rearFrequencyAction() {
        toggleFrequencyBTN(rearFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getRearPeriod());
    }

    private void toggleFrequencyBTN(JFXButton b) {
        // save data
        if (currentFrequency.equals(highFrequency)) {
            azkarSettings.setHighPeriod(azkarPeriod.getValue());
        } else if (currentFrequency.equals(midFrequency)) {
            azkarSettings.setMidPeriod(azkarPeriod.getValue());
        } else if (currentFrequency.equals(lowFrequency)) {
            azkarSettings.setLowPeriod(azkarPeriod.getValue());
        } else if (currentFrequency.equals(rearFrequency)) {
            azkarSettings.setRearPeriod(azkarPeriod.getValue());
        }
        // toggle style to selected button
        currentFrequency.getStyleClass().remove("frequency-btn-selected");
        currentFrequency = b;
        currentFrequency.getStyleClass().add("frequency-btn-selected");
    }

    @FXML
    private void save() {
        try {
            highFrequency.fire();
            azkarSettings.setMorningAzkarReminder(morningAzkarTimeComboBox.getValue());
            azkarSettings.setNightAzkarReminder(nightAzkarTimeComboBox.getValue());
            azkarSettings.setAudioName(azkarAlarmComboBox.getValue());
            azkarSettings.setStopped(stopAzkar.isSelected());
            azkarSettings.save();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
        close();
    }

    @FXML
    private void close() {
        ((Stage) azkarPeriod.getScene().getWindow()).close();
    }

    @FXML
    private void goToAzkar() {
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/views/azkar/absolute/AbsoluteAzkar.fxml"))));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            HelperMethods.SetIcon(stage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToAzkar()");
        }
    }
}
