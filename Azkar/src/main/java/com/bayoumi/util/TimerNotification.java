package com.bayoumi.util;

import com.bayoumi.util.gui.notfication.Notification;
import javafx.application.Platform;

import java.util.Timer;
import java.util.TimerTask;

public class TimerNotification {
    public static Timer timer;
    public static TimerTask timerTask;

    public static void create(long start, long period) {
        if (timer != null) {
//            timerTask.cancel();
            timer.cancel();
            timer.purge();
        }
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() ->
                        Notification.create("اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ على نَبِيِّنَا مُحمَّد", null));

                System.out.println(Thread.currentThread().getName());
            }
        };
        timer.schedule(timerTask, start, period);
    }

}
