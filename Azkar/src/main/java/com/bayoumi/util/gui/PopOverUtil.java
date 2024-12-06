package com.bayoumi.util.gui;

import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.PopOver;

public class PopOverUtil {
    public static void setPopOverAndDisable(ButtonBase node, String text) {
        node.setOpacity(0.5);
        node.setCursor(Cursor.DEFAULT);
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);
        PopOverUtil.init(node, text);
    }

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
