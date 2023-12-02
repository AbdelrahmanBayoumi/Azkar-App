package com.bayoumi.models.settings;

public enum PreferencesType {
    NOTIFICATION_BORDER_COLOR("notification_border_color"),
    NOTIFICATION_POS("notification_pos");
    private final String name;

    PreferencesType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}


