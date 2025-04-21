package com.bayoumi.controllers.settings.azkar;

import com.bayoumi.storage.preferences.PreferencesType;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.NotificationColor;
import com.bayoumi.models.settings.NotificationSettings;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.ColorUtil;
import com.jfoenix.controls.JFXColorPicker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ChooseNotificationColorController implements Initializable {

    public void setData() {
        try {
            final NotificationSettings notificationSettings = Settings.getInstance().getNotificationSettings();
            final String currentBorderColor = notificationSettings.getBorderColor();
            final String currentBackgroundColor = notificationSettings.getBackgroundColor();
            final String currentTextColor = notificationSettings.getTextColor();
            setColor(currentBorderColor, currentBackgroundColor, currentTextColor);
            backgroundColorPicker.setValue(Color.web(currentBackgroundColor));
            borderColorPicker.setValue(Color.web(currentBorderColor));
            textColorPicker.setValue(Color.web(currentTextColor));
        } catch (Exception ignored) {
            setColor(PreferencesType.NOTIFICATION_BORDER_COLOR.getDefaultValue(), PreferencesType.NOTIFICATION_BACKGROUND_COLOR.getDefaultValue(),
                    PreferencesType.NOTIFICATION_TEXT_COLOR.getDefaultValue());
        }
    }

    private String chosenBorderColorHex, chosenBackgroundColorHex, chosenTextColorHex;
    @FXML
    private AnchorPane notificationParent;
    @FXML
    private HBox restoreDefaultColorBox;
    @FXML
    private Label text, title, backgroundColorLabel, borderColorLabel, textColorLabel, defaultThemeLabel;
    @FXML
    private Button saveButton, defaultLightThemeButton, defaultDarkThemeButton;
    @FXML
    private JFXColorPicker backgroundColorPicker, borderColorPicker, textColorPicker;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        LanguageBundle.getInstance().addObserver((o, arg) ->
                updateBundle(LanguageBundle.getInstance().getResourceBundle()));
    }

    public void updateBundle(ResourceBundle bundle) {
        saveButton.setText(Utility.toUTF(bundle.getString("save")));
        title.setText(Utility.toUTF(bundle.getString("settings.azkar.notificationColor")));
        backgroundColorLabel.setText(Utility.toUTF(bundle.getString("backgroundColor")));
        borderColorLabel.setText(Utility.toUTF(bundle.getString("borderColor")));
        textColorLabel.setText(Utility.toUTF(bundle.getString("textColor")));
        restoreDefaultColorBox.setNodeOrientation(NodeOrientation.valueOf(Utility.toUTF(bundle.getString("dir"))));
        defaultThemeLabel.setText(Utility.toUTF(bundle.getString("restoreDefaultColor")));
        defaultLightThemeButton.setText(Utility.toUTF(bundle.getString("lightTheme")));
        defaultDarkThemeButton.setText(Utility.toUTF(bundle.getString("darkTheme")));
    }


    private void setColor(String borderColorHex, String backgroundColorHex, String textColorHex) {
        notificationParent.setStyle("-fx-border-color: " + borderColorHex + ";-fx-background-color: " + backgroundColorHex + ";-fx-border-width: 5;-fx-border-radius: 15;-fx-background-radius: 18;");
        text.setStyle("-fx-text-fill: " + textColorHex + ";");
        this.chosenBorderColorHex = borderColorHex;
        this.chosenBackgroundColorHex = backgroundColorHex;
        this.chosenTextColorHex = textColorHex;
        backgroundColorPicker.setValue(Color.web(backgroundColorHex));
        borderColorPicker.setValue(Color.web(borderColorHex));
        textColorPicker.setValue(Color.web(textColorHex));
    }

    @FXML
    private void chooseBackgroundColorPick() {
        setColor(chosenBorderColorHex, ColorUtil.toHEXCode(backgroundColorPicker.getValue()), chosenTextColorHex);
    }

    @FXML
    private void chooseBorderColorPick() {
        setColor(ColorUtil.toHEXCode(borderColorPicker.getValue()), chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void chooseTextColorPick() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, ColorUtil.toHEXCode(textColorPicker.getValue()));
    }

    @FXML
    private void selectDarkTheme() {
        setColor(NotificationColor.DARK_THEME.getBorderColor(), NotificationColor.DARK_THEME.getBackgroundColor(), NotificationColor.DARK_THEME.getTextColor());
    }

    @FXML
    private void selectLightTheme() {
        setColor(NotificationColor.LIGHT_THEME.getBorderColor(), NotificationColor.LIGHT_THEME.getBackgroundColor(), NotificationColor.LIGHT_THEME.getTextColor());
    }

    @FXML
    private void save() {
        Settings.getInstance().getNotificationSettings().setBorderColor(chosenBorderColorHex);
        Settings.getInstance().getNotificationSettings().setBackgroundColor(chosenBackgroundColorHex);
        Settings.getInstance().getNotificationSettings().setTextColor(chosenTextColorHex);
        ((Stage) backgroundColorPicker.getScene().getWindow()).close();
    }

    @FXML
    private void color000000_BG() {
        setColor(chosenBorderColorHex, "#000000", chosenTextColorHex);
    }

    @FXML
    private void color018AFF_BG() {
        setColor(chosenBorderColorHex, "#018AFF", chosenTextColorHex);
    }

    @FXML
    private void color9AD712_BG() {
        setColor(chosenBorderColorHex, "#9AD712", chosenTextColorHex);
    }

    @FXML
    private void colorA168CE_BG() {
        setColor(chosenBorderColorHex, "#A168CE", chosenTextColorHex);
    }

    @FXML
    private void colorE67999_BG() {
        setColor(chosenBorderColorHex, "#E67999", chosenTextColorHex);
    }

    @FXML
    private void colorE9C46A_BG() {
        setColor(chosenBorderColorHex, "#E9C46A", chosenTextColorHex);
    }

    @FXML
    private void colorFFFFFF_BG() {
        setColor(chosenBorderColorHex, "#FFFFFF", chosenTextColorHex);
    }

    @FXML
    private void color000000_Border() {
        setColor("#000000", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void color018AFF_Border() {
        setColor("#018AFF", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void color9AD712_Border() {
        setColor("#9AD712", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void colorA168CE_Border() {
        setColor("#A168CE", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void colorE67999_Border() {
        setColor("#E67999", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void colorE9C46A_Border() {
        setColor("#E9C46A", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void colorFFFFFF_Border() {
        setColor("#FFFFFF", chosenBackgroundColorHex, chosenTextColorHex);
    }

    @FXML
    private void color000000_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#000000");
    }

    @FXML
    private void color018AFF_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#018AFF");
    }

    @FXML
    private void color9AD712_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#9AD712");
    }

    @FXML
    private void colorA168CE_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#A168CE");
    }

    @FXML
    private void colorE67999_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#E67999");
    }

    @FXML
    private void colorE9C46A_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#E9C46A");
    }

    @FXML
    private void colorFFFFFF_Text() {
        setColor(chosenBorderColorHex, chosenBackgroundColorHex, "#FFFFFF");
    }
}
