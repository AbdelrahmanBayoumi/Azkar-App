package com.bayoumi.util.services.reminders;

import com.batoulapps.adhan.PrayerTimes;
import com.bayoumi.Launcher;
import com.bayoumi.models.AzkarSettings;
import com.bayoumi.models.PrayerTimeSettings;
import com.bayoumi.util.prayertimes.PrayerTimesUtil;
import javafx.scene.image.Image;

import java.text.SimpleDateFormat;

public class ReminderService {

    public static void init(AzkarSettings azkarSettings) {
        AzkarReminderService.clearAllTasks();
        initTimedAzkar(azkarSettings);
        // for testing
//        AzkarReminderService.create(LocalTime.of(1, 37).toString(), "أذكار المساء", null, () -> Launcher.homeController.goToNightAzkar());
    }

    /**
     * init timed azkar (MorningAzkar, NightAzkar)
     *
     * @param azkarSettings to get the offset when the alarm will be displayed
     */
    private static void initTimedAzkar(AzkarSettings azkarSettings) {
        Image morningImage = new Image("/com/bayoumi/images/sun_50px.png");
        Image nightImage = new Image("/com/bayoumi/images/night_50px.png");

        PrayerTimes prayerTimesForToday = PrayerTimesUtil.getPrayerTimesToday(new PrayerTimeSettings());
        if (azkarSettings.getMorningAzkarOffset() != 0) {
            prayerTimesForToday.fajr.setTime(prayerTimesForToday.fajr.getTime() + (azkarSettings.getMorningAzkarOffset() * 60000));
            AzkarReminderService.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.fajr), "أذكار الصباح", morningImage, () -> Launcher.homeController.goToMorningAzkar());
        }
        if (azkarSettings.getNightAzkarOffset() != 0) {
            prayerTimesForToday.asr.setTime(prayerTimesForToday.asr.getTime() + (azkarSettings.getNightAzkarOffset() * 60000));
            AzkarReminderService.create(new SimpleDateFormat("HH:mm").format(prayerTimesForToday.asr), "أذكار المساء", nightImage, () -> Launcher.homeController.goToNightAzkar());
        }
    }
}
