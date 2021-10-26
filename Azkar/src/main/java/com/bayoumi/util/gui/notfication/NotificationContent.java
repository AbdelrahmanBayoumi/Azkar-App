package com.bayoumi.util.gui.notfication;

import javafx.scene.image.Image;

public class NotificationContent {
    private final String text;
    private final Image image;

    public NotificationContent(String text, Image image) {
        this.text = text;
        this.image = image;
    }

    public String getText() {
        return text;
    }

    public Image getImage() {
        return image;
    }
}
