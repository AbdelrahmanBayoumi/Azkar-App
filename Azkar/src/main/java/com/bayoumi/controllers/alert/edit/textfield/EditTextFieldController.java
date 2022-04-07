package com.bayoumi.controllers.alert.edit.textfield;

import com.jfoenix.controls.JFXTextField;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class EditTextFieldController implements Initializable {

    public String returnValue;
    @FXML
    private JFXTextField textfield;

    public void setData(String prompt, String value) {// TODO: handle localization for this view
        textfield.setPromptText(prompt);
        textfield.setText(value);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        returnValue = "";
    }

    @FXML
    private void confirmAction(Event event) {
        if (!textfield.getText().trim().equals("")) {
            returnValue = textfield.getText().trim();
            ((Stage) (((Node) (event.getSource())).getScene().getWindow())).close();
        }
    }

    @FXML
    private void discardAction(Event event) {
        returnValue = "";
        ((Stage) (((Node) (event.getSource())).getScene().getWindow())).close();
    }
}
