package com.bayoumi.models.preferences;

public enum PreferencesType {
    // ======= Notification Settings =======
    NOTIFICATION_BORDER_COLOR("notification_border_color", "#E9C46A"),
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
    TIMED_AZKAR_FONT_SIZE("timed_azkar_font_size", "23"),
    TIMED_AZKAR_DATA_VERSION("timed_azkar_data_version", "0.0.0"),
    // ======= Other Settings =======
    LANGUAGE("language", "ar"),
    HIJRI_OFFSET("hijri_offset", "5"),
    ENABLE_DARK_MODE("enable_dark_mode", "false"),
    ENABLE_24_FORMAT("enable_24_format", "false"),
    MINIMIZED("minimized", "false"),
    AUTOMATIC_CHECK_FOR_UPDATES("automatic_check_for_updates", "true"),
    DB_VERSION("db_version", "0");

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

}


