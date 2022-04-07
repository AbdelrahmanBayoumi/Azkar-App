package com.bayoumi.controllers.dialog;

import com.bayoumi.models.UpdateInfo;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class UpdateConfirmController implements Initializable {
    public boolean isConfirmed;
    @FXML
    private Label oldVersion;
    @FXML
    private Label newVersion;
    @FXML
    private TextArea comment;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
// TODO: handle localization for this view
    }

    public void setData(UpdateInfo updateInfo, String oldVersionValue) {
        oldVersion.setText(oldVersionValue);
        newVersion.setText(updateInfo.getVersion());
        comment.setText(updateInfo.getComment());
    }

    @FXML
    private void confirmAction() {
        isConfirmed = true;
        ((Stage) (oldVersion.getScene().getWindow())).close();
    }

    @FXML
    private void discardAction() {
        isConfirmed = false;
        ((Stage) (oldVersion.getScene().getWindow())).close();
    }

}
