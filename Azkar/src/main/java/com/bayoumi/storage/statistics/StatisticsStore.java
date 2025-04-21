package com.bayoumi.storage.statistics;

import com.bayoumi.storage.KeyValueStore;

import java.util.Arrays;
import java.util.List;

/**
 * Store for application usage statistics
 */
public class StatisticsStore extends KeyValueStore<StatisticsType> {
    private static StatisticsStore instance;

    public static synchronized StatisticsStore getInstance() {
        if (instance == null) {
            instance = new StatisticsStore();
        }
        return instance;
    }

    private StatisticsStore() {
        super("statistics", StatisticsType.class);
    }

    @Override
    protected List<StatisticsType> getKeysThatDoNotAllowedToHaveEmptyValues() {
        // All statistics values must be numeric, so none can be empty
        return Arrays.asList(StatisticsType.values());
    }

    /**
     * Increment the counter for a specific statistic
     *
     * @param key the statistic to increment
     * @return the new value
     */
    public void increment(StatisticsType key) {
        int currentValue = getInt(key);
        set(key, String.valueOf(currentValue + 1));
    }

    /**
     * Reset all statistics to their default values (0)
     */
    public void resetAll() {
        for (StatisticsType key : StatisticsType.values()) {
            set(key, key.getDefaultValue());
        }
    }
}
