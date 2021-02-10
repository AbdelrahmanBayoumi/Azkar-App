package com.bayoumi.models;

import com.bayoumi.util.gui.notfication.Notification;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.util.TimerTask;

public class AzkarReminderTask extends TimerTask {

    private final String text;
    private final Image image;

    public AzkarReminderTask(String text, Image image) {
        this.text = text;
        this.image = image;
    }

    @Override
    public void run() {
        Platform.runLater(() -> Notification.create(text, image));
    }
}
