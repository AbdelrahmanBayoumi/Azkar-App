package com.bayoumi.models;

import com.bayoumi.util.Utilities;
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
//        System.out.println("AzkarReminderTask: run() " + Thread.currentThread().getName()); // TODO DELETE SOUT
        Platform.runLater(() -> Notification.create(text, image));
    }
}
