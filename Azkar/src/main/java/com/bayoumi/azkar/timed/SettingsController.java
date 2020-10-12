package com.bayoumi.azkar.timed;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    private ObservableList<Text> list;

    public void setTextList(ObservableList<Text> list) {
        this.list = list;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
}
