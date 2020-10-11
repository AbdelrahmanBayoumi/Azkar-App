package com.bayoumi.util.gui;

import com.bayoumi.alert.confirm.ConfirmAlertController;
import com.bayoumi.alert.edit.textfield.EditTextFieldController;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author Bayoumi
 */
public class BuilderUI {

    public static boolean showConfirmAlert(boolean isDanger, String text) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            Utility.SetIcon(stage);
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource("/com/bayoumi/fxml/alert/confirm/ConfirmAlert.fxml"));
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
            Utility.SetIcon(stage);
            FXMLLoader loader = new FXMLLoader(BuilderUI.class.getResource("/com/bayoumi/fxml/alert/edit/textfield/EditTextField.fxml"));
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
/*
    private static final String DEFAULT_TITLE = "PS System - Code Clinic";

    public static Image infoImage = new Image("/img/infoPNG.png");
    public static Image appImage = new Image(ControlPanel.getInstance().PATH_OF_LAUNCHER_IMAGE);
    private static final MediaPlayer sound = new MediaPlayer(new Media(new File("audio/timeFinished.mp3").toURI().toString()));

    public static Alert buildErrorAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert a = new Alert(alertType);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        return a;
    }

    public static void showNotification(Node icon, String msg) {
        icon.setStyle("-fx-font-size:80;");
        Label emailLabel = new Label(msg);
        emailLabel.setAlignment(Pos.CENTER);
        VBox vb = new VBox(icon, emailLabel);
        vb.setAlignment(Pos.CENTER);
        vb.setSpacing(5);
        Notifications notification = Notifications.create()
                .graphic(vb)
                .hideAfter(Duration.seconds(10))
                .position(Pos.BOTTOM_RIGHT);
        notification.show();
    }

    public static Stage initStageDecorated(Scene scene, String title, String Icon) {
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(title == null ? DEFAULT_TITLE : title);
        stage.initModality(Modality.APPLICATION_MODAL);
        setIcon(stage, Icon);
        return stage;
    }

    public static Stage initStageTransparent(Scene scene, String title, String Icon) {
        scene.setFill(Color.TRANSPARENT);
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title == null ? DEFAULT_TITLE : title);
        setIcon(stage, Icon);
        return stage;
    }

    public static Stage initStageUnDecorated(Scene scene, String title, String Icon) {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle(title == null ? DEFAULT_TITLE : title);
        stage.initModality(Modality.APPLICATION_MODAL);
        setIcon(stage, Icon);
        return stage;
    }

    public static void setAppDecoration(Stage s) {
        s.setTitle(DEFAULT_TITLE);
        setIcon(s, "");
    }

    public static void setIcon(Stage s, String iconName) {
        s.getIcons().clear();
        Image icon;
        if (iconName.equalsIgnoreCase("info")) {
            icon = infoImage;
        } else {
            icon = appImage;
        }
        s.getIcons().add(icon);
    }

    public static void HandlerCTRL_W(Scene scene, Stage stage, Runnable rn) {
        HashMap<KeyCombination, Runnable> hashMap = new HashMap<>();
        // CTRL + W
        KeyCombination kc1 = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
        hashMap.put(kc1, rn);
        scene.getAccelerators().putAll(hashMap);
    }

    public static void showTimerNotification(String roomName) {
        MaterialDesignIconView icon = new MaterialDesignIconView(MaterialDesignIcon.TIMER);
        icon.setStyle("-fx-font-size:80;");
        Label msg = new Label("انتهى وقت  : " + roomName);
        msg.setAlignment(Pos.CENTER);
        VBox vb = new VBox(msg, icon);
        vb.setAlignment(Pos.CENTER);
        vb.setSpacing(5);
        Notifications notification = Notifications.create()
                .graphic(vb)
                .hideAfter(Duration.seconds(10))
                .position(Pos.BOTTOM_RIGHT)
                .onAction((ActionEvent event) -> {
                    sound.stop();
                });
        notification.show();
        sound.play();
    }
    */
}
