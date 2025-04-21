package com.bayoumi.storage.preferences;

import com.bayoumi.storage.DatabaseManager;
import com.bayoumi.storage.KeyValueStore;
import com.bayoumi.util.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
