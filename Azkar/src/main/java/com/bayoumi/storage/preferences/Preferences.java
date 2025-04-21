package com.bayoumi.storage.preferences;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.services.update.UpdateHandler;
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
        checkForUpdate();
    }

    private void checkForUpdate() {
        // TODO: move this check to a service
        Logger.debug("Start automaticCheckForUpdates");
        final Timer timer = new Timer();
        timer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Logger.debug("automaticCheckForUpdates: run()");
                        if (UpdateHandler.getInstance().checkUpdate() == 1 & Settings.getInstance().getAutomaticCheckForUpdates()) {
                            UpdateHandler.getInstance().showInstallPrompt();
                        }
                        // close the thread
                        timer.cancel();
                    }
                },
                390000 // 6.5min => to ensure that update will open when no notification is shown
        );
    }


    @Override
    protected List<PreferencesType> getKeysThatDoNotAllowedToHaveEmptyValues() {
        return Arrays.asList(PreferencesType.ADHAN_AUDIO, PreferencesType.AUDIO_NAME);
    }


    public Map<String, String> getAllWithPrefix() {
        return super.getAll("preferences.");
    }


    /**
     * Fetches the raw string values for the given keys, then maps them through the provided parser.
     *
     * @param keys   the prefs keys to fetch
     * @param parser function that turns the raw String into T
     * @return map of PreferencesType → parsed T
     */
    public <T> Map<PreferencesType, T> getValues(Collection<PreferencesType> keys, Function<String, T> parser) {
        // TODO remove this
        // build SQL: SELECT key, value FROM preferences WHERE key IN (?, ?, …)
        final String placeholders = keys.stream().map(k -> "?").collect(Collectors.joining(","));
        final String sql = "SELECT key, value FROM preferences WHERE key IN (" + placeholders + ")";
        final Map<PreferencesType, T> result = new EnumMap<>(PreferencesType.class);

        try (PreparedStatement ps = DatabaseManager.getInstance().con.prepareStatement(sql)) {
            int i = 1;
            for (PreferencesType k : keys) {
                ps.setString(i++, k.getName());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final PreferencesType pt = PreferencesType.fromKey(rs.getString("key"));
                    final String raw = rs.getString("value");
                    result.put(pt, parser.apply(raw));
                }
            }
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".getValues()");
        }
        return result;
    }

}
