package com.bayoumi.preloader;

import com.jfoenix.controls.JFXProgressBar;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    @FXML
    public JFXProgressBar progressBarInstance;
    @FXML
    public Label progressLabel;

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    public void RootMousePressed(Event event) {
        final MouseEvent e = (MouseEvent) event;
        xOffset = e.getSceneX();
        yOffset = e.getSceneY();
    }

    @FXML
    public void RootMouseDragged(Event event) {
        final MouseEvent e = (MouseEvent) event;
        ((Node) (event.getSource())).getScene().getWindow().setX(e.getScreenX() - xOffset);
        ((Node) (event.getSource())).getScene().getWindow().setY(e.getScreenY() - yOffset);
    }
}
