package com.bayoumi.models.settings;

public class Settings {

    private static Settings instance = null;
    private final AzkarSettings azkarSettings;
    private final PrayerTimeSettings prayerTimeSettings;
    private final NotificationSettings notificationSettings;

    private Settings() {
        azkarSettings = new AzkarSettings();
        prayerTimeSettings = new PrayerTimeSettings();
        notificationSettings = new NotificationSettings();
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public AzkarSettings getAzkarSettings() {
        return azkarSettings;
    }

    public PrayerTimeSettings getPrayerTimeSettings() {
        return prayerTimeSettings;
    }

    public NotificationSettings getNotificationSettings() {
        return notificationSettings;
    }
}
