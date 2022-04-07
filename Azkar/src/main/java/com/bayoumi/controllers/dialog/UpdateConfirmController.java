package com.bayoumi.controllers.dialog;

import com.bayoumi.models.UpdateInfo;
import com.bayoumi.models.settings.LanguageBundle;
import com.bayoumi.util.Utility;
import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class UpdateConfirmController implements Initializable {
    public boolean isConfirmed;
    @FXML
    private Label thereIsANewUpdate;

    @FXML
    private Label oldVersion;

    @FXML
    private Label oldVersionText;

    @FXML
    private Label newVersion;

    @FXML
    private Label newVersionText;

    @FXML
    private Label notesText;
    @FXML
    private Label doYouWantToUpdateTheSoftware;
    @FXML
    private JFXButton discardButton;
    @FXML
    private JFXButton confirmBTN;
    @FXML
    private TextArea comment;
    @FXML
    private VBox root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setData(UpdateInfo updateInfo, String oldVersionValue) {
        oldVersion.setText(oldVersionValue);
        if (updateInfo != null) {
            newVersion.setText(updateInfo.getVersion());
            comment.setText(updateInfo.getComment());
        }

        final ResourceBundle bundle = LanguageBundle.getInstance().getResourceBundle();
        confirmBTN.setText(Utility.toUTF(bundle.getString("confirm")));
        discardButton.setText(Utility.toUTF(bundle.getString("discard")));
        thereIsANewUpdate.setText(Utility.toUTF(bundle.getString("thereIsANewUpdate")));
        oldVersionText.setText(Utility.toUTF(bundle.getString("oldVersionText")));
        newVersionText.setText(Utility.toUTF(bundle.getString("newVersionText")));
        notesText.setText(Utility.toUTF(bundle.getString("notes")) + ":");
        doYouWantToUpdateTheSoftware.setText(Utility.toUTF(bundle.getString("doYouWantToUpdateTheSoftware")));

        root.setNodeOrientation(NodeOrientation.valueOf(Utility.toUTF(bundle.getString("dir"))));
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
