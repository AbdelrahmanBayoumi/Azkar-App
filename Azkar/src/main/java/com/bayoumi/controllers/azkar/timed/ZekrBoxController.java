package com.bayoumi.controllers.azkar.timed;

import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXButton;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.scene.text.Text;


public class ZekrBoxController {
    private int repeatValue;
    @FXML
    private Text text;
    @FXML
    private JFXButton repeatBTN;
    @FXML
    private JFXButton copyBTN;
    @FXML
    private FontAwesomeIconView copyIcon;

    private void updateRepeatButton(int repeat) {
        this.repeatBTN.setText("التكرار" + "\t" + repeat);
    }

    public void setData(String text, int repeatValue) {
        this.text.setText(text);
        this.repeatValue = repeatValue;
        updateRepeatButton(this.repeatValue);
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
            updateRepeatButton(this.repeatValue);
            this.repeatBTN.setDisable(true);
            // remove box
//            ((Pane) text.getParent().getParent().getParent().getParent()).getChildren().remove(text.getParent().getParent().getParent());
        }
    }
}
