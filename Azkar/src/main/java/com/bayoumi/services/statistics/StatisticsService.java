package com.bayoumi.services.statistics;

import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.preferences.PreferencesType;
import com.bayoumi.util.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for incrementing and retrieving statistics stored in Preferences.
 * All write operations are offloaded to a background thread to avoid blocking the JavaFX UI.
 */
public class StatisticsService {

    private static StatisticsService instance;
    private final Preferences prefs;
    private final ExecutorService executor;

    private StatisticsService() {
        this.prefs = Preferences.getInstance();
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable, "StatisticsService-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Returns the singleton instance of StatisticsService.
     */
    public static synchronized StatisticsService getInstance() {
        if (instance == null) {
            instance = new StatisticsService();
        }
        return instance;
    }

    /**
     * Increments the stored integer value for the given PreferencesType key.
     * This is performed on a background thread to avoid blocking the UI.
     *
     * @param key the statistic key to increment
     */
    public void increment(final PreferencesType key) {
        executor.submit(() -> {
            try {
                int current = prefs.getInt(key);
                int updated = current + 1;
                prefs.set(key, String.valueOf(updated));
                Logger.debug("[StatisticsService] " + key + " incremented to " + updated);
            } catch (Exception e) {
                Logger.error(null, e, getClass().getName() + ".increment()");
            }
        });
    }


    /**
     * Shuts down the internal executor service. Call on application exit if desired.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
