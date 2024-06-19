package com.bayoumi.util.gui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.controlsfx.control.PopOver;

public class PopOverUtil {
    public static void init(Node node, String text) {
        final Label label = new Label(text);
        label.getStyleClass().add("pop-over-label");
        label.setMaxHeight(Double.MAX_VALUE);
        final PopOver popOver = new PopOver(label);
        popOver.setCloseButtonEnabled(true);
        node.setOnMouseEntered(mouseEvent -> popOver.show(node));
        node.setOnMouseExited(mouseEvent -> popOver.hide());
    }
}
