package com.bayoumi.controllers.settings.azkar;

import com.bayoumi.models.AzkarSettings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.HelperMethods;
import com.bayoumi.util.gui.IntegerSpinner;
import com.bayoumi.util.time.Utilities;
import com.bayoumi.util.validation.SingleInstance;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    private Label minPlurality;
    @FXML
    private Spinner<Integer> azkarPeriod_hour;
    @FXML
    private Label hourPlurality;
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
        IntegerSpinner.init(azkarPeriod);
        IntegerSpinner.init(azkarPeriod_hour);
        azkarPeriod.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 1));
        azkarPeriod_hour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        azkarPeriod.valueProperty().addListener((observable, oldValue, newValue) -> minPlurality.setText(Utilities.minutesArabicPlurality(Integer.parseInt(azkarPeriod.getEditor().getText()))));
        azkarPeriod_hour.valueProperty().addListener((observable, oldValue, newValue) -> hourPlurality.setText(Utilities.hoursArabicPlurality(Integer.parseInt(azkarPeriod_hour.getEditor().getText()))));
        azkarPeriod.setOnKeyReleased(event -> minPlurality.setText(Utilities.minutesArabicPlurality(Integer.parseInt(azkarPeriod.getEditor().getText()))));
        azkarPeriod_hour.setOnKeyReleased(event -> hourPlurality.setText(Utilities.hoursArabicPlurality(Integer.parseInt(azkarPeriod_hour.getEditor().getText()))));

        // init morning and night azkar reminder
        nightAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));
        morningAzkarTimeComboBox.setItems(FXCollections.observableArrayList("لا تذكير", "بـ نصف ساعة", "بـ ساعة"));

        // init Saved data form DB
        azkarSettings = new AzkarSettings();
        morningAzkarTimeComboBox.setValue(azkarSettings.getMorningAzkarReminder());
        nightAzkarTimeComboBox.setValue(azkarSettings.getNightAzkarReminder());
        azkarAlarmComboBox.setValue(azkarSettings.getAudioName());
        azkarAlarmComboBox.setItems(AzkarSettings.getAudioList());

        azkarPeriod.getValueFactory().setValue(azkarSettings.getHighPeriod() % 60);
        azkarPeriod_hour.getValueFactory().setValue(azkarSettings.getHighPeriod() / 60);

        stopAzkar.setSelected(azkarSettings.isStopped());
        currentFrequency = highFrequency;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        // disable unsupported features
//        nightAzkarTimeComboBox.setDisable(true);
//        morningAzkarTimeComboBox.setDisable(true);
    }

    @FXML
    private void highFrequencyAction() {
        toggleFrequencyBTN(highFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getHighPeriod() % 60);
        azkarPeriod_hour.getValueFactory().setValue(azkarSettings.getHighPeriod() / 60);
    }

    @FXML
    private void lowFrequencyAction() {
        toggleFrequencyBTN(lowFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getLowPeriod() % 60);
        azkarPeriod_hour.getValueFactory().setValue(azkarSettings.getLowPeriod() / 60);
    }

    @FXML
    private void midFrequencyAction() {
        toggleFrequencyBTN(midFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getMidPeriod() % 60);
        azkarPeriod_hour.getValueFactory().setValue(azkarSettings.getMidPeriod() / 60);
    }

    @FXML
    private void rearFrequencyAction() {
        toggleFrequencyBTN(rearFrequency);
        azkarPeriod.getValueFactory().setValue(azkarSettings.getRearPeriod() % 60);
        azkarPeriod_hour.getValueFactory().setValue(azkarSettings.getRearPeriod() / 60);
    }

    private void toggleFrequencyBTN(JFXButton b) {
        azkarPeriod.getValueFactory().setValue(Integer.valueOf(azkarPeriod.getEditor().getText()));
        azkarPeriod_hour.getValueFactory().setValue(Integer.valueOf(azkarPeriod_hour.getEditor().getText()));
        if (azkarPeriod.getEditor().getText().equals("0") && azkarPeriod_hour.getEditor().getText().equals("0")) {
            azkarPeriod.getValueFactory().setValue(1);
        }
        // save data
        if (currentFrequency.equals(highFrequency)) {
            azkarSettings.setHighPeriod(azkarPeriod.getValueFactory().getValue() + azkarPeriod_hour.getValueFactory().getValue() * 60);
        } else if (currentFrequency.equals(midFrequency)) {
            azkarSettings.setMidPeriod(azkarPeriod.getValueFactory().getValue() + azkarPeriod_hour.getValueFactory().getValue() * 60);
        } else if (currentFrequency.equals(lowFrequency)) {
            azkarSettings.setLowPeriod(azkarPeriod.getValueFactory().getValue() + azkarPeriod_hour.getValueFactory().getValue() * 60);
        } else if (currentFrequency.equals(rearFrequency)) {
            azkarSettings.setRearPeriod(azkarPeriod.getValueFactory().getValue() + azkarPeriod_hour.getValueFactory().getValue() * 60);
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
