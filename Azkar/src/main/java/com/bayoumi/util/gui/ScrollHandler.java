package com.bayoumi.util.gui;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

public class ScrollHandler {
    public static void init(Node node, ScrollPane scrollPane, double factor) {
        node.setOnScroll(event -> {
            double deltaY = event.getDeltaY() * factor;
            double width = scrollPane.getContent().getBoundsInLocal().getWidth();
            double vvalue = scrollPane.getVvalue();
            scrollPane.setVvalue(vvalue + -deltaY / width);
        });
    }
}
