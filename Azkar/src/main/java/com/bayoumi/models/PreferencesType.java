package com.bayoumi.models;

public enum PreferencesType {
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


