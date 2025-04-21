package com.bayoumi.storage.preferences;

import com.bayoumi.storage.KeyValueStore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Preferences extends KeyValueStore<PreferencesType> {
    private static Preferences instance;

    public synchronized static Preferences getInstance() {
        return instance;
    }

    public static void init() throws Exception {
        if (instance == null) {
            instance = new Preferences();
        }
    }

    private Preferences() {
        super("preferences", PreferencesType.class);
    }

    @Override
    protected List<PreferencesType> getKeysThatDoNotAllowedToHaveEmptyValues() {
        return Arrays.asList(PreferencesType.ADHAN_AUDIO, PreferencesType.AUDIO_NAME);
    }


    public Map<String, String> getAllWithPrefix() {
        return super.getAll("preferences.");
    }
}
