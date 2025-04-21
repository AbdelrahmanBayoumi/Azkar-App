//package com.bayoumi.services.statistics;
//
//import com.bayoumi.storage.preferences.Preferences;
//import com.bayoumi.storage.preferences.PreferencesType;
//import com.bayoumi.util.Utility;
//
//import java.util.Collections;
//import java.util.EnumSet;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * Immutable snapshot of all statistic counts, loaded once from the database.
// */
//public final class StatisticsSnapshot {
//    private static final Set<PreferencesType> STATISTICS_KEYS = Collections.unmodifiableSet(EnumSet.of(
//            PreferencesType.SETTINGS_OPEN_STATISTICS,
//            PreferencesType.SETTINGS_AZKAR_STATISTICS,
//            PreferencesType.SETTINGS_PRAYERS_STATISTICS,
//            PreferencesType.SETTINGS_OTHER_STATISTICS,
//            PreferencesType.SETTINGS_COLORS_STATISTICS,
//            PreferencesType.SETTINGS_AZKAR_DB_STATISTICS,
//            PreferencesType.TIMED_AZKAR_SETTINGS_STATISTICS,
//            PreferencesType.MORNING_AZKAR_OPEN_STATISTICS,
//            PreferencesType.NIGHT_AZKAR_OPEN_STATISTICS,
//            PreferencesType.MORNING_AZKAR_NOTIFICATION_STATISTICS,
//            PreferencesType.MORNING_AZKAR_NOTIFICATION_CLICK_STATISTICS,
//            PreferencesType.NIGHT_AZKAR_NOTIFICATION_STATISTICS,
//            PreferencesType.NIGHT_AZKAR_NOTIFICATION_CLICK_STATISTICS,
//            PreferencesType.OTHER_PRAYER_TIMES_OPEN_STATISTICS,
//            PreferencesType.AZKAR_NOTIFICATION_STATISTICS,
//            PreferencesType.AZKAR_NOTIFICATION_CLICK_STATISTICS
//    ));
//
//    private final Map<PreferencesType, Integer> stats;
//
//    private StatisticsSnapshot(Map<PreferencesType, Integer> stats) {
//        this.stats = stats;
//    }
//
//    /**
//     * Loads all preference entries for statistics in a single DB hit,
//     * and returns an immutable snapshot.
//     */
//    public static StatisticsSnapshot load() {
//        return new StatisticsSnapshot(Collections.unmodifiableMap(Preferences.getInstance()
//                .getValues(STATISTICS_KEYS, Utility::parseIntOrZero)));
//    }
//
//    /**
//     * Returns the count for the given statistic key, or 0 if absent.
//     */
//    public int getCount(PreferencesType key) {
//        return stats.getOrDefault(key, 0);
//    }
//
//    /**
//     * Returns an unmodifiable view of all statistic counts.
//     */
//    public Map<PreferencesType, Integer> asMap() {
//        return stats;
//    }
//
//    @Override
//    public String toString() {
//        return stats.entrySet().stream()
//                .map(e -> e.getKey().getName() + "=" + e.getValue())
//                .collect(Collectors.joining(", ", "StatisticsSnapshot[", "]"));
//    }
//
//}
