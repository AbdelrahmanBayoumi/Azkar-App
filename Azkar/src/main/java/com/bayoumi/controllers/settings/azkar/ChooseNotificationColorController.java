package com.bayoumi.controllers.settings.azkar;

import com.bayoumi.models.Preferences;
import com.bayoumi.models.PreferencesType;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ColorUtil;
import com.jfoenix.controls.JFXColorPicker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ChooseNotificationColorController implements Initializable {

    public void setData(String currentBorderColor) {
        try {
            setColor(currentBorderColor);
            colorPicker.setValue(Color.web(currentBorderColor));
        } catch (Exception ignored) {
            colorPicker.setValue(Color.web(Constants.NOTIFICATION_BORDER_COLOR));
        }
    }

    private void setColor(String hexColor) {
        notificationParent.setStyle("-fx-border-color: " + hexColor + ";-fx-border-width: 5;-fx-border-radius: 15;");
        this.chosenColorHex = hexColor;
    }

    private String chosenColorHex;
    @FXML
    private AnchorPane notificationParent;
    @FXML
    private HBox notificationBox;
    @FXML
    private Label text, title;
    @FXML
    private Button closeButton, saveButton;
    @FXML
    private JFXColorPicker colorPicker;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        LanguageBundle.getInstance().addObserver((o, arg) ->
                updateBundle(LanguageBundle.getInstance().getResourceBundle()));
    }

    public void updateBundle(ResourceBundle bundle) {
        saveButton.setText(Utility.toUTF(bundle.getString("save")));
        title.setText(Utility.toUTF(bundle.getString("settings.azkar.notificationColor")));
    }

    @FXML
    private void chooseColorPicker() {
        setColor(ColorUtil.toHEXCode(colorPicker.getValue()));
    }

    @FXML
    private void save() throws Exception {
        Preferences.getInstance().set(PreferencesType.NOTIFICATION_BORDER_COLOR, chosenColorHex);
        ((Stage) colorPicker.getScene().getWindow()).close();
    }


    @FXML
    private void color000000() {
        setColor("#000000");
    }

    @FXML
    private void color018AFF() {
        setColor("#018AFF");
    }

    @FXML
    private void color9AD712() {
        setColor("#9AD712");
    }

    @FXML
    private void colorA168CE() {
        setColor("#A168CE");
    }

    @FXML
    private void colorE67999() {
        setColor("#E67999");
    }

    @FXML
    private void colorE9C46A() {
        setColor("#E9C46A");
    }

}
