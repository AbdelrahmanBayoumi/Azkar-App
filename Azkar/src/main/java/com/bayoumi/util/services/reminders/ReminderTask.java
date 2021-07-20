package com.bayoumi.util.services.reminders;

import com.bayoumi.models.NotificationSettings;
import com.bayoumi.util.gui.notfication.Notification;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;

public class ReminderTask {
    private static Timer ALARM = new Timer();

    public static void create(String time, String text, Image image, Runnable callback, NotificationSettings notificationSettings) {
        try {
            System.out.println("time: " + time);
            if (LocalTime.parse(time).isAfter(LocalTime.now()) || LocalTime.parse(time).equals(LocalTime.now())) { // 86400000
                ALARM.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> Notification.createControlsFX(text, image, callback, 30, notificationSettings));
                    }
                    // 86400000 Milliseconds = 24 Hour
                }, new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(LocalDate.now() + " " + LocalTime.parse(time)), 86400000);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void create(String time, String text, Image image, Runnable callback, double duration, NotificationSettings notificationSettings) {
        try {
            System.out.println("time: " + time);
            if (LocalTime.parse(time).isAfter(LocalTime.now()) || LocalTime.parse(time).equals(LocalTime.now())) { // 86400000
                ALARM.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> Notification.createControlsFX(text, image, callback, duration, notificationSettings));
                    }
                }, new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(LocalDate.now() + " " + LocalTime.parse(time)), 86400000);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void clearAllTasks() {
        if (ALARM != null) {
            ALARM.cancel();
            ALARM.purge();
        }
        ALARM = null;
        ALARM = new Timer();
    }

}
