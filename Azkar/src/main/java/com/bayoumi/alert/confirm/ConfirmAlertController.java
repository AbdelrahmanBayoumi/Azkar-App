package com.bayoumi.alert.confirm;

import com.jfoenix.controls.JFXButton;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfirmAlertController implements Initializable {
    public boolean isConfirmed;
    @FXML
    private Label text;
    @FXML
    private JFXButton confirmBTN;

    public void setData(boolean isDanger, String text) {
        this.text.setText(text);
        if (isDanger) {
            confirmBTN.getStyleClass().add("close-button");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isConfirmed = false;
    }

    @FXML
    private void confirmAction(Event event) {
        isConfirmed = true;
        ((Stage) (((Node) (event.getSource())).getScene().getWindow())).close();
    }

    @FXML
    private void discardAction(Event event) {
        isConfirmed = false;
        ((Stage) (((Node) (event.getSource())).getScene().getWindow())).close();
    }
}
