package com.bayoumi.util.gui;

import com.bayoumi.controllers.alert.confirm.ConfirmAlertController;
import com.bayoumi.controllers.alert.edit.textfield.EditTextFieldController;
import com.bayoumi.controllers.dialog.UpdateConfirmController;
import com.bayoumi.models.UpdateInfo;
import com.bayoumi.util.Logger;
import com.bayoumi.util.validation.SingleInstance;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author Bayoumi
 */
public class BuilderUI {

    public static boolean showUpdateDetails(UpdateInfo updateInfo, String currentVersion) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource("/com/bayoumi/views/dialog/UpdateConfirm.fxml"));
            stage.setScene(new Scene(loader.load()));
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

    public static void showOkAlert(Alert.AlertType alertType, String text, boolean isRTL) {
        Alert alert = new Alert(alertType);
        alert.setTitle("");
        alert.setHeaderText(null);
        alert.setContentText(text);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(BuilderUI.class.getResource("/com/bayoumi/css/style.css").toExternalForm());
        if (isRTL) {
            (dialogPane.getChildren().get(1)).setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }
        HelperMethods.SetIcon((Stage) alert.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    public static boolean showConfirmAlert(boolean isDanger, String text) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            HelperMethods.SetIcon(stage);
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource("/com/bayoumi/views/alert/confirm/ConfirmAlert.fxml"));
            stage.setScene(new Scene(loader.load()));
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
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource("/com/bayoumi/views/alert/edit/textfield/EditTextField.fxml"));
            stage.setScene(new Scene(loader.load()));
            EditTextFieldController controller = loader.getController();
            controller.setData(prompt, value);
            stage.showAndWait();
            return controller.returnValue;
        } catch (Exception ex) {
            Logger.error(null, ex, BuilderUI.class.getName() + ".showEditTextField()");
            return "";
        }
    }

}
