package com.bayoumi.controllers.alert.edit.textfield;

import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXTextField;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class EditTextFieldController implements Initializable {

    public String returnValue;
    @FXML
    private JFXTextField textfield;
    @FXML
    private Button discardButton, confirmBTN;

    public void setData(String prompt, String value) {
        final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
        confirmBTN.setText(Utility.toUTF(bundle.getString("confirm")));
        discardButton.setText(Utility.toUTF(bundle.getString("discard")));
        textfield.setNodeOrientation(NodeOrientation.valueOf(Utility.toUTF(bundle.getString("dir"))));

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
