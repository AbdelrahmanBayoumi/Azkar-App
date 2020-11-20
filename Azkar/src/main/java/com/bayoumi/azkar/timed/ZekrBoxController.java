package com.bayoumi.azkar.timed;

import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;


public class ZekrBoxController {

    private int repeatValue;
    @FXML
    private Text text;
    @FXML
    private JFXButton repeatBTN;

    public void setData(String text, int repeatValue) {
        this.text.setText(text);
        this.repeatValue = repeatValue;
        this.repeatBTN.setText("التكرار" + "     " + "(" + this.repeatValue + ")");
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
            ((Pane) text.getParent().getParent().getParent().getParent()).getChildren().remove(text.getParent().getParent().getParent());
        }
    }
}
