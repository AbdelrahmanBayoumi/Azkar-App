package com.bayoumi.util.services.reminders;

import com.batoulapps.adhan.PrayerTimes;
import com.bayoumi.Launcher;
import com.bayoumi.models.AzkarSettings;
import com.bayoumi.models.NotificationSettings;
import com.bayoumi.models.PrayerTimeSettings;
import com.bayoumi.util.prayertimes.PrayerTimesUtil;
import javafx.scene.image.Image;

import java.text.SimpleDateFormat;

public class ReminderService {

    public static void init(AzkarSettings azkarSettings, PrayerTimeSettings prayerTimeSettings, NotificationSettings notificationSettings) {
        ReminderTask.clearAllTasks();
        final PrayerTimes prayerTimesForToday = PrayerTimesUtil.getPrayerTimesToday(prayerTimeSettings);
        initPrayerTimes(prayerTimesForToday, notificationSettings);
        initTimedAzkar(azkarSettings, prayerTimesForToday, notificationSettings);
        // for testing
//        ReminderTask.create(LocalTime.of(1, 37).toString(), "أذكار المساء", null, () -> Launcher.homeController.goToNightAzkar());
    }

    /**
     * init timed azkar (MorningAzkar, NightAzkar)
     *
     * @param azkarSettings       to get the offset when the alarm will be displayed
     * @param prayerTimesForToday to get prayer times to get time of fajr and asr
     */
    private static void initTimedAzkar(AzkarSettings azkarSettings, PrayerTimes prayerTimesForToday, NotificationSettings notificationSettings) {
        System.out.println("Timed Azkar Reminder ...");
        if (azkarSettings.getMorningAzkarOffset() != 0) {
            prayerTimesForToday.fajr.setTime(prayerTimesForToday.fajr.getTime() + (azkarSettings.getMorningAzkarOffset() * 60000));
            ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.fajr), "أذكار الصباح", new Image("/com/bayoumi/images/sun_50px.png"), () -> Launcher.homeController.goToMorningAzkar(), notificationSettings);
        }
        if (azkarSettings.getNightAzkarOffset() != 0) {
            prayerTimesForToday.asr.setTime(prayerTimesForToday.asr.getTime() + (azkarSettings.getNightAzkarOffset() * 60000));
            ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.asr), "أذكار المساء", new Image("/com/bayoumi/images/night_50px.png"), () -> Launcher.homeController.goToNightAzkar(), notificationSettings);
        }
    }

    /**
     * initializes reminders for prayer times
     *
     * @param prayerTimesForToday to get all prayer times
     */
    private static void initPrayerTimes(PrayerTimes prayerTimesForToday, NotificationSettings notificationSettings) {
        System.out.println("PrayerTimes Reminder ...");
        final Image image = new Image("/com/bayoumi/images/Kaaba.png");
        ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.fajr), "صلاة الفجر", image, null, 300, notificationSettings);
        ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.sunrise), "شروق الشمس", new Image("/com/bayoumi/images/sun_50px.png"), null, 300, notificationSettings);
        ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.dhuhr), "صلاة الظهر", image, null, 300, notificationSettings);
        ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.asr), "صلاة العصر", image, null, 300, notificationSettings);
        ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.maghrib), "صلاة المغرب", image, null, 300, notificationSettings);
        ReminderTask.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.isha), "صلاة العشاء", image, null, 300, notificationSettings);
    }
}
