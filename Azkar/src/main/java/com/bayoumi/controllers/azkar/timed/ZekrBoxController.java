package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXButton;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

import java.util.ResourceBundle;


public class ZekrBoxController {
    private ResourceBundle bundle;
    private int repeatValue;
    @FXML
    private Text text;
    @FXML
    private JFXButton repeatBTN;
    @FXML
    private JFXButton copyBTN;
    @FXML
    private FontAwesomeIconView copyIcon;

    private void updateBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        copyBTN.setText(Utility.toUTF(bundle.getString("copy")));
        repeatBTN.setText(Utility.toUTF(bundle.getString("repetition")));
    }

    private void updateRepeatButton(int repeat) {
        this.repeatBTN.setText((Utility.toUTF(bundle.getString("repetition")) + "  " + "(" + repeat + ")"));
    }

    public void setData(String text, int repeatValue) {
        this.text.setText(text);
        updateFontSize();
        this.repeatValue = repeatValue;
        updateBundle(LanguageBundle.getInstance().getResourceBundle());
        updateRepeatButton(this.repeatValue);
    }

    public void updateFontSize() {
        System.out.println("[ZekrBoxController] updateFontSize()");
        this.text.setStyle("-fx-font-family: 'Noto Naskh Arabic'; -fx-font-weight: BOLD; -fx-text-alignment: center;-fx-font-size: " + Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize() + ";");
    }

    @FXML
    private void copy() {
        Utility.copyToClipboard(text.getText());
    }

    @FXML
    private void repeat() {
        if (this.repeatBTN.isDisable()) {
            return;
        }
        if (this.repeatValue > 1) {
            updateRepeatButton(--this.repeatValue);
        } else {
            (this.repeatBTN.getParent()).setStyle("-fx-background-color: linear-gradient(-fx-gray, lightgray); -fx-background-radius:  0 0 20 20;");
            this.repeatBTN.setStyle("-fx-text-fill: -fx-secondary;");
            this.copyBTN.setStyle("-fx-text-fill: -fx-secondary;");
            copyIcon.setStyle("-fx-fill: -fx-secondary;");
            updateRepeatButton(--this.repeatValue);
            this.repeatBTN.setDisable(true);
        }
    }

}
