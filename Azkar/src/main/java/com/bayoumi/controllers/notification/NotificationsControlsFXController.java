package com.bayoumi.controllers.notification;

import com.bayoumi.util.gui.Draggable;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationsControlsFXController implements Initializable {
    @FXML
    private AnchorPane parent;
    @FXML
    private HBox notificationBox;
    @FXML
    private Label text;
    @FXML
    public Button closeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Draggable(notificationBox);
        text.setText("");

        notificationBox.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                notificationBox.getScene().getStylesheets().remove("/com/bayoumi/css/style.css");
                if (notificationBox.getScene().getStylesheets().isEmpty()) {
                    notificationBox.getScene().getStylesheets().add(0, "/com/bayoumi/css/controlsfx-notification.css");
                }
            }
        });
        closeButton.setVisible(false);
        parent.setOnMouseEntered(event -> closeButton.setVisible(true));
        parent.setOnMouseExited(event -> closeButton.setVisible(false));
    }

    public void setData(String msg, Image image) {
        this.text.setText(msg);
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            notificationBox.getChildren().add(0, imageView);
        }
    }

}
