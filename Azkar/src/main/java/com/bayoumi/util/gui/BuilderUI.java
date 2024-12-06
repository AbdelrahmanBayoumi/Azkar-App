package com.bayoumi.util.gui;

import com.bayoumi.controllers.alert.confirm.ConfirmAlertController;
import com.bayoumi.controllers.alert.edit.textfield.EditTextFieldController;
import com.bayoumi.controllers.dialog.UpdateConfirmController;
import com.bayoumi.models.UpdateInfo;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.gui.load.Locations;
import com.bayoumi.util.validation.SingleInstance;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Bayoumi
 */
public class BuilderUI {

    public static boolean showUpdateDetails(UpdateInfo updateInfo, String currentVersion) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource(Locations.UpdateConfirm.toString()));
            Scene scene = new Scene(loader.load());
            // TODO: test theme on this view
            scene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            stage.setScene(scene);
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            UpdateConfirmController controller = loader.getController();
            controller.setData(updateInfo, currentVersion);
            stage.showAndWait();
            return controller.isConfirmed;
        } catch (Exception ex) {
            Logger.error(null, ex, BuilderUI.class.getName() + ".showUpdateDetails()");
            return false;
        }
    }

    public static void showOkAlert(Alert.AlertType alertType, String text, ResourceBundle resourceBundle) {
        Alert alert = new Alert(alertType);
        alert.setTitle("");
        alert.setHeaderText(null);
        alert.setContentText(text);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().addAll(Settings.getInstance().getThemeFilesCSS());
        if (Utility.toUTF(resourceBundle.getString("dir")).equals("rtl")) {
            (dialogPane.getChildren().get(1)).setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText(Utility.toUTF(resourceBundle.getString("Ok")));
        }
        HelperMethods.SetIcon((Stage) alert.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    public static boolean showConfirmAlert(boolean isDanger, String text) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource(Locations.ConfirmAlert.toString()));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            stage.setScene(scene);
            ConfirmAlertController controller = loader.getController();
            controller.setData(isDanger, text);
            stage.showAndWait();
            return controller.isConfirmed;
        } catch (Exception ex) {
            Logger.error(null, ex, BuilderUI.class.getName() + ".showConfirmAlert()");
            return false;
        }

    }

    public static String showEditTextField(String prompt, String value) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource(Locations.EditTextField.toString()));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().setAll(Settings.getInstance().getThemeFilesCSS());
            stage.setScene(scene);
            EditTextFieldController controller = loader.getController();
            controller.setData(prompt, value);
            stage.showAndWait();
            return controller.returnValue;
        } catch (Exception ex) {
            Logger.error(null, ex, BuilderUI.class.getName() + ".showEditTextField()");
            return "";
        }
    }

    public static Stage initStageDecorated(Scene scene, String title) {
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(title == null ? "" : title);
        stage.initModality(Modality.APPLICATION_MODAL);
        HelperMethods.SetIcon(stage);
        return stage;
    }
}
