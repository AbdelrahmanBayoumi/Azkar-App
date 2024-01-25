package com.bayoumi.models.preferences;

public enum PreferencesType {
    // Notification Settings
    NOTIFICATION_BORDER_COLOR("notification_border_color", "#E9C46A"),
    NOTIFICATION_POS("notification_pos", "BOTTOM_LEFT"),
    // Other Settings
    LANGUAGE("language", "ar"),
    ENABLE_DARK_MODE("enable_dark_mode", "false"),
    ENABLE_24_FORMAT("enable_24_format", "false"),
    MINIMIZED("minimized", "false"),
    HIJRI_OFFSET("hijri_offset", "0"),
    AUTOMATIC_CHECK_FOR_UPDATES("automatic_check_for_updates", "true"),
    IS_MANUAL_LOCATION_SELECTED("IS_MANUAL_LOCATION_SELECTED", "true");

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


