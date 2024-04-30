package com.bayoumi.models.settings;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesObservable;
import com.bayoumi.models.preferences.PreferencesType;

import java.util.AbstractMap.SimpleEntry;

// TODO: remove notifyObservers() values that doesn't need observers
public class Settings extends PreferencesObservable {

    // ==== Singleton Pattern ====
    private static Settings instance = null;

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    // =================================================================
    private final AzkarSettings azkarSettings;
    private final PrayerTimeSettings prayerTimeSettings;
    private final NotificationSettings notificationSettings;
    private final SimpleEntry<PreferencesType, Boolean> automaticCheckForUpdates;
    private final SimpleEntry<PreferencesType, Boolean> nightMode;
    private final SimpleEntry<PreferencesType, Boolean> enable24Format;
    private final SimpleEntry<PreferencesType, Boolean> minimized;
    private final SimpleEntry<PreferencesType, Language> language;
    private final SimpleEntry<PreferencesType, Integer> hijriOffset;


    private Settings() {
        azkarSettings = new AzkarSettings();
        prayerTimeSettings = new PrayerTimeSettings();
        notificationSettings = new NotificationSettings();
        automaticCheckForUpdates = new SimpleEntry<>(PreferencesType.AUTOMATIC_CHECK_FOR_UPDATES, Boolean.valueOf(PreferencesType.AUTOMATIC_CHECK_FOR_UPDATES.getDefaultValue()));
        nightMode = new SimpleEntry<>(PreferencesType.ENABLE_DARK_MODE, Boolean.valueOf(PreferencesType.ENABLE_DARK_MODE.getDefaultValue()));
        enable24Format = new SimpleEntry<>(PreferencesType.ENABLE_24_FORMAT, Boolean.valueOf(PreferencesType.ENABLE_24_FORMAT.getDefaultValue()));
        minimized = new SimpleEntry<>(PreferencesType.MINIMIZED, Boolean.valueOf(PreferencesType.MINIMIZED.getDefaultValue()));
        language = new SimpleEntry<>(PreferencesType.LANGUAGE, Language.get(PreferencesType.LANGUAGE.getDefaultValue()));
        hijriOffset = new SimpleEntry<>(PreferencesType.HIJRI_OFFSET, Integer.valueOf(PreferencesType.HIJRI_OFFSET.getDefaultValue()));

        loadSettings();
    }

    public void loadSettings() {
        automaticCheckForUpdates.setValue(Preferences.getInstance().getBoolean(automaticCheckForUpdates.getKey()));
        nightMode.setValue(Preferences.getInstance().getBoolean(nightMode.getKey()));
        enable24Format.setValue(Preferences.getInstance().getBoolean(enable24Format.getKey()));
        minimized.setValue(Preferences.getInstance().getBoolean(minimized.getKey()));
        language.setValue(Language.get(Preferences.getInstance().get(language.getKey())));
        hijriOffset.setValue(Preferences.getInstance().getInt(hijriOffset.getKey()));
    }

    public int getHijriOffset() {
        return hijriOffset.getValue();
    }

    public void setHijriOffset(int value) {
        // 1. set value to local variable
        hijriOffset.setValue(value);
        // 2. save value to DB
        Preferences.getInstance().set(hijriOffset.getKey(), value + "");
        // 3. notify observers
        notifyObservers(hijriOffset.getKey(), value);
    }

    public Language getLanguage() {
        return language.getValue();
    }

    public void setLanguage(String languageLocal) {
        // 1. set value to local variable
        language.setValue(Language.get(languageLocal));
        // 2. save value to DB
        Preferences.getInstance().set(language.getKey(), languageLocal);
        // 3. notify observers
        notifyObservers(language.getKey(), Language.get(languageLocal));
    }

    public boolean getMinimized() {
        return minimized.getValue();
    }

    public void setMinimized(boolean value) {
        // 1. set value to local variable
        minimized.setValue(value);
        // 2. save value to DB
        Preferences.getInstance().set(minimized.getKey(), value + "");
        // 3. notify observers
        notifyObservers(minimized.getKey(), value);
    }

    public boolean getEnable24Format() {
        return enable24Format.getValue();
    }

    public void setEnable24Format(boolean value) {
        // 1. set value to local variable
        enable24Format.setValue(value);
        // 2. save value to DB
        Preferences.getInstance().set(enable24Format.getKey(), value + "");
        // 3. notify observers
        notifyObservers(enable24Format.getKey(), value);
    }

    public boolean getNightMode() {
        return nightMode.getValue();
    }

    public void setNightMode(boolean value) {
        // 1. set value to local variable
        nightMode.setValue(value);
        // 2. save value to DB
        Preferences.getInstance().set(nightMode.getKey(), value + "");
        // 3. notify observers
        notifyObservers(nightMode.getKey(), value);
    }

    public boolean getAutomaticCheckForUpdates() {
        return automaticCheckForUpdates.getValue();
    }

    public void setAutomaticCheckForUpdates(boolean value) {
        // 1. set value to local variable
        automaticCheckForUpdates.setValue(value);
        // 2. save value to DB
        Preferences.getInstance().set(automaticCheckForUpdates.getKey(), value + "");
        // 3. notify observers
        notifyObservers(automaticCheckForUpdates.getKey(), value);
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
