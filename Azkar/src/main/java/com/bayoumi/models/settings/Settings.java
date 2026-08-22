package com.bayoumi.models.settings;

import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.preferences.PreferencesObservable;
import com.bayoumi.storage.preferences.PreferencesType;

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
    private final SimpleEntry<PreferencesType, Theme> theme;
    private final SimpleEntry<PreferencesType, Boolean> enable24Format;
    private final SimpleEntry<PreferencesType, Boolean> minimized;
    private final SimpleEntry<PreferencesType, Language> language;
    private final SimpleEntry<PreferencesType, Integer> hijriOffset;
    private final SimpleEntry<PreferencesType, Boolean> sendUsageData;


    private Settings() {
        azkarSettings = new AzkarSettings();
        prayerTimeSettings = new PrayerTimeSettings();
        notificationSettings = new NotificationSettings();
        automaticCheckForUpdates = new SimpleEntry<>(PreferencesType.AUTOMATIC_CHECK_FOR_UPDATES, Boolean.valueOf(PreferencesType.AUTOMATIC_CHECK_FOR_UPDATES.getDefaultValue()));
        theme = new SimpleEntry<>(PreferencesType.ENABLE_DARK_MODE, Theme.from(PreferencesType.ENABLE_DARK_MODE.getDefaultValue()));
        enable24Format = new SimpleEntry<>(PreferencesType.ENABLE_24_FORMAT, Boolean.valueOf(PreferencesType.ENABLE_24_FORMAT.getDefaultValue()));
        minimized = new SimpleEntry<>(PreferencesType.MINIMIZED, Boolean.valueOf(PreferencesType.MINIMIZED.getDefaultValue()));
        language = new SimpleEntry<>(PreferencesType.LANGUAGE, Language.get(PreferencesType.LANGUAGE.getDefaultValue()));
        hijriOffset = new SimpleEntry<>(PreferencesType.HIJRI_OFFSET, Integer.valueOf(PreferencesType.HIJRI_OFFSET.getDefaultValue()));
        sendUsageData = new SimpleEntry<>(PreferencesType.SEND_USAGE_DATA, Boolean.valueOf(PreferencesType.SEND_USAGE_DATA.getDefaultValue()));

        loadSettings();
    }

    public void loadSettings() {
        automaticCheckForUpdates.setValue(Preferences.getInstance().getBoolean(automaticCheckForUpdates.getKey()));
        theme.setValue(Theme.from(Preferences.getInstance().get(theme.getKey())));
        enable24Format.setValue(Preferences.getInstance().getBoolean(enable24Format.getKey()));
        minimized.setValue(Preferences.getInstance().getBoolean(minimized.getKey()));
        language.setValue(Language.get(Preferences.getInstance().get(language.getKey())));
        hijriOffset.setValue(Preferences.getInstance().getInt(hijriOffset.getKey()));
        sendUsageData.setValue(Preferences.getInstance().getBoolean(sendUsageData.getKey()));
    }

    public boolean getSendUsageData() {
        return sendUsageData.getValue();
    }

    public void setSendUsageData(boolean value) {
        // 1. set value to local variable
        sendUsageData.setValue(value);
        // 2. save value to DB
        Preferences.getInstance().set(sendUsageData.getKey(), value + "");
        // 3. notify observers
        notifyObservers(sendUsageData.getKey(), value);
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

    public Theme getTheme() {
        return theme.getValue();
    }

    public void setTheme(Theme value) {
        if (value == null || value == theme.getValue()) {
            return;
        }
        theme.setValue(value);
        Preferences.getInstance().set(theme.getKey(), value.getPreferenceValue());
        notifyObservers(theme.getKey(), value);
    }

    public boolean getNightMode() {
        return theme.getValue().isDark();
    }

    public void setNightMode(boolean value) {
        setTheme(value ? Theme.DARK : Theme.LIGHT);
    }

    public void syncDefaultNotificationColors() {
        if (theme.getValue() != Theme.SYSTEM) {
            return;
        }
        final NotificationSettings notification = getNotificationSettings();
        final boolean usingLightDefaults =
                NotificationColor.LIGHT_THEME.getBackgroundColor().equals(notification.getBackgroundColor())
                        && NotificationColor.LIGHT_THEME.getTextColor().equals(notification.getTextColor())
                        && NotificationColor.LIGHT_THEME.getBorderColor().equals(notification.getBorderColor());
        final boolean usingDarkDefaults =
                NotificationColor.DARK_THEME.getBackgroundColor().equals(notification.getBackgroundColor())
                        && NotificationColor.DARK_THEME.getTextColor().equals(notification.getTextColor())
                        && NotificationColor.DARK_THEME.getBorderColor().equals(notification.getBorderColor());
        if (getNightMode() && usingLightDefaults) {
            NotificationColor.setDarkTheme();
        } else if (!getNightMode() && usingDarkDefaults) {
            NotificationColor.setLightTheme();
        }
    }

    public String[] getThemeFilesCSS() {
        return new String[]{
                "/com/bayoumi/css/base.css",
                getNightMode() ? "/com/bayoumi/css/dark-theme.css" : "/com/bayoumi/css/light-theme.css"
        };
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
