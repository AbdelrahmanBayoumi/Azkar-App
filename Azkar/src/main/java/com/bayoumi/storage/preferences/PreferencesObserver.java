package com.bayoumi.storage.preferences;

public interface PreferencesObserver {
    void update(PreferencesType key, Object value);
}
