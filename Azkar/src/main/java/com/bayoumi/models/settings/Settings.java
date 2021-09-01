package com.bayoumi.models.settings;

public class Settings {

    private static Settings instance = null;
    private final AzkarSettings azkarSettings;
    private final PrayerTimeSettings prayerTimeSettings;
    private final NotificationSettings notificationSettings;
    private final OtherSettings otherSettings;

    private Settings() {
        azkarSettings = new AzkarSettings();
        prayerTimeSettings = new PrayerTimeSettings();
        notificationSettings = new NotificationSettings();
        otherSettings = new OtherSettings();
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

    public OtherSettings getOtherSettings() {
        return otherSettings;
    }
}
