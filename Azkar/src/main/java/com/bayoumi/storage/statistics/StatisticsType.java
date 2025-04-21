package com.bayoumi.storage.statistics;

import com.bayoumi.storage.KeyValueDefault;

public enum StatisticsType implements KeyValueDefault {
    // ======= Settings Statistics =======
    SETTINGS_OPENED("settings_opened", "0"),
    SETTINGS_AZKAR_OPENED("settings_azkar_opened", "0"),
    SETTINGS_PRAYERS_OPENED("settings_prayers_opened", "0"),
    SETTINGS_OTHER_OPENED("settings_other_opened", "0"),
    SETTINGS_COLORS_OPENED("settings_colors_opened", "0"),
    SETTINGS_AZKAR_DB_OPENED("settings_azkar_db_opened", "0"),
    SETTINGS_TIMED_AZKAR_OPENED("settings_timed_azkar_opened", "0"),

    // ======= Azkar Statistics =======
    MORNING_AZKAR_OPENED("morning_azkar_opened", "0"),
    NIGHT_AZKAR_OPENED("night_azkar_opened", "0"),
    MORNING_AZKAR_NOTIFICATION_SHOWN("morning_azkar_notification_shown", "0"),
    MORNING_AZKAR_NOTIFICATION_CLICKED("morning_azkar_notification_clicked", "0"),
    NIGHT_AZKAR_NOTIFICATION_SHOWN("night_azkar_notification_shown", "0"),
    NIGHT_AZKAR_NOTIFICATION_CLICKED("night_azkar_notification_clicked", "0"),
    AZKAR_NOTIFICATION_SHOWN("azkar_notification_shown", "0"),
    AZKAR_NOTIFICATION_CLICKED("azkar_notification_clicked", "0"),

    // ======= Prayer Times Statistics =======
    PRAYER_TIMES_OTHER_OPENED("prayer_times_other_opened", "0");

    private final String name;
    private final String defaultValue;

    StatisticsType(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
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
    public static StatisticsType fromKey(String key) {
        for (StatisticsType st : values()) {
            if (st.getName().equals(key)) {
                return st;
            }
        }
        throw new IllegalArgumentException("Unknown StatisticsType key: " + key);
    }
}
