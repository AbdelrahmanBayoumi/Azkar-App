package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ResourceBundle;


public class SettingsController {

    @FXML
    private Label fontSizeLabel, changeFontSizeLabel;
    @FXML
    private Text textPreview;
    @FXML
    private Button minimizeFontButton;

    private JFXDialog dialog;
    private boolean isChanged = false;

    private void updateBundle(ResourceBundle bundle) {
        changeFontSizeLabel.setText(Utility.toUTF(bundle.getString("settings.azkar.changeFontSize")));
    }

    public void setData(JFXDialog dialog, Runnable onCloseAction) {
        this.dialog = dialog;
        isChanged = false;

        textPreview.setFont(Font.font(Constants.QURAN_FONT_FAMILY, FontWeight.BOLD, Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize()));
        fontSizeLabel.setText(String.valueOf(Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize()));

        dialog.setOnDialogClosed(event -> {
            if (isChanged) {
                if (onCloseAction != null) onCloseAction.run();
                Settings.getInstance().getAzkarSettings().notifyObservers();
            }
        });
        updateBundle(LanguageBundle.getInstance().getResourceBundle());

        minimizeFontButton.requestFocus();
    }

    @FXML
    private void maximizeFont() {
        isChanged = true;
        textPreview.setFont(Font.font(Constants.QURAN_FONT_FAMILY, FontWeight.BOLD, (textPreview.getFont().getSize() + 1) > 60 ? 60 : textPreview.getFont().getSize() + 1));
        fontSizeLabel.setText(String.valueOf((int) textPreview.getFont().getSize()));
        new Thread(() -> Settings.getInstance().getAzkarSettings().setTimedAzkarFontSize((int) textPreview.getFont().getSize())).start();
    }

    @FXML
    private void minimizeFont() {
        isChanged = true;
        textPreview.setFont(Font.font(Constants.QURAN_FONT_FAMILY, FontWeight.BOLD, (textPreview.getFont().getSize() - 1) < 10 ? 10 : textPreview.getFont().getSize() - 1));
        fontSizeLabel.setText(String.valueOf((int) textPreview.getFont().getSize()));
        new Thread(() -> Settings.getInstance().getAzkarSettings().setTimedAzkarFontSize((int) textPreview.getFont().getSize())).start();
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
