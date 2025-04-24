package com.bayoumi.storage.statistics;

import com.bayoumi.storage.DatabaseManager;
import com.bayoumi.storage.KeyValueStore;
import com.bayoumi.util.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Store for application usage statistics
 */
public class StatisticsStore extends KeyValueStore<StatisticsType> {
    private static StatisticsStore instance;
    private static final String TABLE_NAME = "statistics";

    public static synchronized StatisticsStore getInstance() {
        if (instance == null) {
            instance = new StatisticsStore();
        }
        return instance;
    }

    private StatisticsStore() {
        super(TABLE_NAME, StatisticsType.class);
    }

    @Override
    protected List<StatisticsType> getKeysThatDoNotAllowedToHaveEmptyValues() {
        // All statistics values must be numeric, so none can be empty
        return Arrays.asList(StatisticsType.values());
    }

    /**
     * Increment the counter for a specific statistic
     */
    public void increment(StatisticsType key) {
        int currentValue = getInt(key);
        set(key, String.valueOf(currentValue + 1));
    }

    public Map<String, String> getAllWithPrefix() {
        return super.getAll("statistics.");
    }

    /**
     * Reset all statistics to their default values (0)
     */
    public void resetAll() {
        try {
            DatabaseManager.getInstance().con.prepareStatement("UPDATE " + TABLE_NAME + " SET value = 0").execute();
        } catch (Exception ex) {
            Logger.error("Reset failed on " + TABLE_NAME, ex, getClass().getName() + ".resetAll()");
        }
    }
}
