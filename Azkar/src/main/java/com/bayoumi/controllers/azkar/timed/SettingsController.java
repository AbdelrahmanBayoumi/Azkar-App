package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Constants;
import com.jfoenix.controls.JFXDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class SettingsController {

    @FXML
    private Label fontSizeLabel;
    @FXML
    private Text textPreview;

    private JFXDialog dialog;

    public void setData(JFXDialog dialog) {
        // TODO: handle localization for this view
        this.dialog = dialog;

        textPreview.setFont(Font.font(Constants.QURAN_FONT_FAMILY, FontWeight.BOLD, Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize()));
        fontSizeLabel.setText(String.valueOf(Settings.getInstance().getAzkarSettings().getTimedAzkarFontSize()));

        dialog.setOnDialogClosed(event -> Settings.getInstance().getAzkarSettings().notifyObservers());
    }

    @FXML
    private void maximizeFont() {
        textPreview.setFont(Font.font(Constants.QURAN_FONT_FAMILY, FontWeight.BOLD, (textPreview.getFont().getSize() + 1) > 60 ? 60 : textPreview.getFont().getSize() + 1));
        fontSizeLabel.setText(String.valueOf((int) textPreview.getFont().getSize()));
        new Thread(() -> Settings.getInstance().getAzkarSettings().setTimedAzkarFontSize((int) textPreview.getFont().getSize())).start();
    }

    @FXML
    private void minimizeFont() {
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
