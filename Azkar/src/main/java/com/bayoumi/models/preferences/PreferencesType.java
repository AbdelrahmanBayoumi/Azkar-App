package com.bayoumi.models.preferences;

import com.bayoumi.util.db.KeyValueDefault;

public enum PreferencesType implements KeyValueDefault {
    // ======= Notification Settings =======
    NOTIFICATION_BACKGROUND_COLOR("notification_background_color", "#FFFFFF"),
    NOTIFICATION_BORDER_COLOR("notification_border_color", "#E9C46A"),
    NOTIFICATION_TEXT_COLOR("notification_text_color", "#000000"),
    NOTIFICATION_POS("notification_pos", "BOTTOM_LEFT"),

    // ======= Prayer Times Settings =======
    COUNTRY("country", "Egypt"),
    CITY("city", "Cairo"),
    SUMMER_TIMING("summer_timing", "false"),
    METHOD("method", "5"),
    ASR_JURISTIC("asr_juristic", "0"),
    LATITUDE("latitude", "27.556363"),
    LONGITUDE("longitude", "30.807579"),
    IS_MANUAL_LOCATION_SELECTED("IS_MANUAL_LOCATION_SELECTED", "true"),
    ADHAN_AUDIO("adhan_audio", "adhan-abdulbasit-abdusamad.mp3"),

    // ======= Azkar Settings =======
    MORNING_AZKAR_REMINDER("morning_azkar_reminder", "30"),
    NIGHT_AZKAR_REMINDER("night_azkar_reminder", "30"),
    AUDIO_NAME("audio_name", ""),
    SELECTED_PERIOD("selected_period", "high"),
    HIGH_PERIOD("high_period", "5"),
    MID_PERIOD("mid_period", "10"),
    LOW_PERIOD("low_period", "20"),
    REAR_PERIOD("rear_period", "30"),
    IS_AZKAR_STOPPED("is_stopped", "false"),
    VOLUME("volume", "100"),
    PRAYER_VOLUME("prayer_volume", "100"),
    AZKAR_DURATION("azkar_duration", "30"),
    FAJR_ADJUSTMENT("fajr_adjustment", "0"),
    SUNRISE_ADJUSTMENT("sunrise_adjustment", "0"),
    DHUHR_ADJUSTMENT("dhuhr_adjustment", "0"),
    ASR_ADJUSTMENT("asr_adjustment", "0"),
    MAGHRIB_ADJUSTMENT("maghrib_adjustment", "0"),
    ISHAA_ADJUSTMENT("isha_adjustment", "0"),
    TIMED_AZKAR_FONT_SIZE("timed_azkar_font_size", "23"),
    TIMED_AZKAR_DATA_VERSION("timed_azkar_data_version", "0.0.0"),
    IS_PRAYERS_REMINDER_STOPPED("is_prayers_reminder_stopped", "false"),
    // ======= Other Settings =======
    LANGUAGE("language", "ar"),
    HIJRI_OFFSET("hijri_offset", "5"),
    ENABLE_DARK_MODE("enable_dark_mode", "false"),
    ENABLE_24_FORMAT("enable_24_format", "false"),
    MINIMIZED("minimized", "false"),
    AUTOMATIC_CHECK_FOR_UPDATES("automatic_check_for_updates", "true"),
    SEND_USAGE_DATA("send_usage_data", "true"),
    APP_VERSION("app_version", "0"),
    DB_VERSION("db_version", "0"),
    // ======== Statistics ========
    SETTINGS_OPEN_STATISTICS("settings_open_statistics", "0"),
    SETTINGS_AZKAR_STATISTICS("settings_azkar_statistics", "0"),
    SETTINGS_PRAYERS_STATISTICS("settings_prayers_statistics", "0"),
    SETTINGS_OTHER_STATISTICS("settings_other_statistics", "0"),
    SETTINGS_COLORS_STATISTICS("settings_colors_statistics", "0"),
    SETTINGS_AZKAR_DB_STATISTICS("settings_db_statistics", "0"),
    TIMED_AZKAR_SETTINGS_STATISTICS("timed_azkar_settings_statistics", "0"),
    MORNING_AZKAR_OPEN_STATISTICS("morning_azkar_statistics", "0"),
    NIGHT_AZKAR_OPEN_STATISTICS("night_azkar_statistics", "0"),
    MORNING_AZKAR_NOTIFICATION_STATISTICS("morning_azkar_notification_statistics", "0"),
    MORNING_AZKAR_NOTIFICATION_CLICK_STATISTICS("morning_azkar_notification_click_statistics", "0"),
    NIGHT_AZKAR_NOTIFICATION_STATISTICS("night_azkar_notification_statistics", "0"),
    NIGHT_AZKAR_NOTIFICATION_CLICK_STATISTICS("night_azkar_notification_click_statistics", "0"),
    OTHER_PRAYER_TIMES_OPEN_STATISTICS("other_prayer_times_statistics", "0"),
    AZKAR_NOTIFICATION_STATISTICS("azkar_notification_statistics", "0"),
    AZKAR_NOTIFICATION_CLICK_STATISTICS("azkar_notification_click_statistics", "0");

    private final String name;
    private final String defaultValue;

    PreferencesType(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Find the enum whose getName() equals the given key.
     * Throws IllegalArgumentException if none found.
     */
    public static PreferencesType fromKey(String key) {
        for (PreferencesType pt : values()) {
            if (pt.getName().equals(key)) {
                return pt;
            }
        }
        throw new IllegalArgumentException("Unknown PreferencesType key: " + key);
    }
}
