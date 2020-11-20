package com.bayoumi.azkar.timed;

import com.jfoenix.controls.JFXDialog;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class SettingsController {

    private ObservableList<Text> list;
    private JFXDialog dialog;

    public void setData(ObservableList<Text> list, JFXDialog dialog) {
        this.list = list;
        this.dialog = dialog;
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
