package com.bayoumi.notification;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationController implements Initializable {

    @FXML
    private HBox notificationBox;
    @FXML
    private ImageView image;
    @FXML
    private TextFlow textFlow;
    @FXML
    private Text text;

    public void setData(String msg) {
        text.setText(msg);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    private void closeAction(ActionEvent event) {
    }
}
