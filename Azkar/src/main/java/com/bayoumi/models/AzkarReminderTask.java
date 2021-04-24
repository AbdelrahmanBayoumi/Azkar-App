package com.bayoumi.models;

import com.bayoumi.util.gui.notfication.Notification;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.util.TimerTask;

public class AzkarReminderTask extends TimerTask {

    private final String text;
    private final Image image;
    private final Runnable callback;

    public AzkarReminderTask(String text, Image image, Runnable callback) {
        this.text = text;
        this.image = image;
        this.callback = callback;
    }

    @Override
    public void run() {
        Platform.runLater(() -> Notification.createControlsFX(text, image,callback,30));
    }
}
