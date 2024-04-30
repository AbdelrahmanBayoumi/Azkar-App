package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.settings.Settings;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXToggleButton;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class SettingsController {

    @FXML
    private Spinner<Integer> morningAzkarTimeSpinner, nightAzkarTimeSpinner;
    @FXML
    private JFXToggleButton morningAzkarTimeToggle, nightAzkarTimeToggle;

    private ObservableList<Text> list;
    private JFXDialog dialog;

    public void setData(ObservableList<Text> list, JFXDialog dialog) {
        // TODO: handle localization for this view
        this.list = list;
        this.dialog = dialog;

        morningAzkarTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 30, 10));
        nightAzkarTimeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 30, 10));

        morningAzkarTimeSpinner.getValueFactory().setValue(Settings.getInstance().getAzkarSettings().getMorningAzkarReminder());
        nightAzkarTimeSpinner.getValueFactory().setValue(Settings.getInstance().getAzkarSettings().getNightAzkarReminder());

        morningAzkarTimeToggle.setSelected(morningAzkarTimeSpinner.getValueFactory().getValue() != 0);
        nightAzkarTimeToggle.setSelected(nightAzkarTimeSpinner.getValueFactory().getValue() != 0);

        dialog.setOnDialogClosed(event -> Settings.getInstance().getAzkarSettings().notifyObservers());
    }

    @FXML
    private void onMorningAzkarTimeChange() {
        Settings.getInstance().getAzkarSettings().setMorningAzkarReminder(morningAzkarTimeSpinner.getValueFactory().getValue());
        if (morningAzkarTimeSpinner.getValueFactory().getValue() == 0) {
            morningAzkarTimeToggle.setSelected(false);
            morningAzkarTimeSpinner.setDisable(true);
            toggleAction(morningAzkarTimeToggle);
        }
    }

    @FXML
    private void onNightAzkarTimeChange() {
        Settings.getInstance().getAzkarSettings().setNightAzkarReminder(nightAzkarTimeSpinner.getValueFactory().getValue());
        if (nightAzkarTimeSpinner.getValueFactory().getValue() == 0) {
            nightAzkarTimeToggle.setSelected(false);
            nightAzkarTimeSpinner.setDisable(true);
            toggleAction(nightAzkarTimeToggle);
        }
    }

    @FXML
    private void onMorningAzkarTimeToggle() {
        toggleAction(morningAzkarTimeToggle);
        if (morningAzkarTimeToggle.isSelected()) {
            morningAzkarTimeSpinner.setDisable(false);
            morningAzkarTimeSpinner.getValueFactory().setValue(30);
            Settings.getInstance().getAzkarSettings().setMorningAzkarReminder(30);
        } else {
            morningAzkarTimeSpinner.setDisable(true);
            morningAzkarTimeSpinner.getValueFactory().setValue(0);
            Settings.getInstance().getAzkarSettings().setMorningAzkarReminder(0);
        }
    }

    @FXML
    private void onNightAzkarTimeToggle() {
        toggleAction(nightAzkarTimeToggle);
        if (nightAzkarTimeToggle.isSelected()) {
            nightAzkarTimeSpinner.setDisable(false);
            nightAzkarTimeSpinner.getValueFactory().setValue(30);
            Settings.getInstance().getAzkarSettings().setNightAzkarReminder(30);
        } else {
            nightAzkarTimeSpinner.setDisable(true);
            nightAzkarTimeSpinner.getValueFactory().setValue(0);
            Settings.getInstance().getAzkarSettings().setNightAzkarReminder(0);
        }
    }

    private void toggleAction(JFXToggleButton toggleButton) {
        if (toggleButton.isSelected()) {
            toggleButton.setText("مُفعلة");
        } else {
            toggleButton.setText("لا تذكير");
        }
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

}
