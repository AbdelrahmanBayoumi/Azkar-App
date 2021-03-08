package com.bayoumi.util.gui;

import javafx.scene.Node;

public class Draggable {
    private double xOffset = 0;
    private double yOffset = 0;

    public Draggable(Node node) {
        node.setOnMouseDragged(e -> {
            ((Node) (e.getSource())).getScene().getWindow().setX(e.getScreenX() - xOffset);
            ((Node) (e.getSource())).getScene().getWindow().setY(e.getScreenY() - yOffset);
        });
        node.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
    }
}
