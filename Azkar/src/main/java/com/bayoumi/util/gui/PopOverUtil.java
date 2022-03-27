package com.bayoumi.util.gui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.controlsfx.control.PopOver;

public class PopOverUtil {
    public static void init(Node node, String text) {
        Label label = new Label(text);
        label.setStyle("-fx-padding: 10;-fx-background-color: #E9C46A;-fx-text-fill: #000000;-fx-font-weight: bold;-fx-text-alignment: center;-fx-alignment: center;");
        label.setWrapText(true);
        label.setMaxWidth(400);
        PopOver popOver = new PopOver(label);
        popOver.setCloseButtonEnabled(true);
        node.setOnMouseEntered(mouseEvent -> popOver.show(node));
        node.setOnMouseExited(mouseEvent -> popOver.hide());
    }
}
