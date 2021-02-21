package com.bayoumi.models;

import javafx.scene.image.Image;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Timer;

public class AzkarReminderService {
    private static Timer ALARM = new Timer();

    public static void create(String time, String text, Image image) {
        try {
            System.out.println("time: " + time);
            if (LocalTime.parse(time).isAfter(LocalTime.now()) || LocalTime.parse(time).equals(LocalTime.now())) { // 86400000
                ALARM.schedule(new AzkarReminderTask(text, image), new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(LocalDate.now() + " " + LocalTime.parse(time)), 86400000);
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
