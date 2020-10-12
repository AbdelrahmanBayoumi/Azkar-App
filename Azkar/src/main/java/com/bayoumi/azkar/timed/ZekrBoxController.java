package com.bayoumi.azkar.timed;

import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class ZekrBoxController implements Initializable {

    @FXML
    private Text text;
    @FXML
    private JFXButton repeatBTN;
    private int repeatValue;

    public void setData(String text, int repeatValue) {
        this.text.setText(text);
        this.repeatValue = repeatValue;
        this.repeatBTN.setText("التكرار" + "     " + "(" + this.repeatValue + ")");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void copy() {
        Utility.copyToClipboard(text.getText());
    }

    @FXML
    private void repeat() {
        if (this.repeatValue > 1) {
            this.repeatBTN.setText("التكرار" + "     " + "(" + (--this.repeatValue) + ")");
        } else {
            // remove box
            ((Pane) text.getParent().getParent().getParent()).getChildren().remove(text.getParent().getParent());
        }
    }
}
