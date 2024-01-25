package com.bayoumi.models.preferences;

public interface PreferencesObserver {
    void update(PreferencesType key, String value);
}
