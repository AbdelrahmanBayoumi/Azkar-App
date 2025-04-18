package com.bayoumi.services.statistics;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of all statistic counts, loaded once from the database.
 */
public final class StatisticsSnapshot {
    private final Map<PreferencesType, Integer> stats;

    private StatisticsSnapshot(Map<PreferencesType, Integer> stats) {
        this.stats = stats;
    }

    /**
     * Loads all preference entries whose keys end with _COUNT in a single DB hit,
     * and returns an immutable snapshot.
     */
    public static StatisticsSnapshot load() {
        Preferences prefs = Preferences.getInstance();
        Map<String, String> allPrefs = prefs.getAll();

        // Filter only statistics-based statistics
        Set<PreferencesType> statisticsKeys = Arrays.stream(PreferencesType.values())
                .filter(pt -> pt.name().endsWith("_STATISTICS"))
                .collect(Collectors.toSet());

        final Map<PreferencesType, Integer> map = new EnumMap<>(PreferencesType.class);
        for (PreferencesType key : statisticsKeys) {
            String raw = allPrefs.get("preferences." + key.getName());
            int value = 0;
            if (raw != null) {
                try {
                    value = Integer.parseInt(raw);
                } catch (NumberFormatException ignored) {
                }
            }
            map.put(key, value);
        }

        return new StatisticsSnapshot(Collections.unmodifiableMap(map));
    }

    /**
     * Returns the count for the given statistic key, or 0 if absent.
     */
    public int getCount(PreferencesType key) {
        return stats.getOrDefault(key, 0);
    }

    /**
     * Returns an unmodifiable view of all statistic counts.
     */
    public Map<PreferencesType, Integer> asMap() {
        return stats;
    }

}
