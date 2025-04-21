package com.bayoumi.services.statistics;

import com.bayoumi.storage.statistics.StatisticsStore;
import com.bayoumi.storage.statistics.StatisticsType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for incrementing and retrieving statistics stored in Preferences.
 * All write operations are offloaded to a background thread to avoid blocking the JavaFX UI.
 */
public class StatisticsService {

    private static StatisticsService instance;
    private final StatisticsStore store;
    private final ExecutorService executor;

    private StatisticsService() {
        this.store = StatisticsStore.getInstance();
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
    public void increment(final StatisticsType key) {
        executor.submit(() -> store.increment(key));
    }

    /**
     * Shuts down the internal executor service. Call on application exit if desired.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
