package com.bayoumi.services.statistics;

import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.preferences.PreferencesType;
import com.bayoumi.storage.statistics.StatisticsStore;
import com.bayoumi.util.AppPropertiesUtil;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import kong.unirest.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class WeeklyStatsManager {

    /**
     * If we've moved into a fresh week, return the old week's start & stats, and then rollover.
     * Otherwise, return null.
     */
    public static WeeklyStats oldWeekIfRolling(boolean sendUsageData) {
        final Instant computedWeekStart = computeWeekStart();
        final Instant storedWeekStart = loadOrInitWeekStart(computedWeekStart);

        final long weeksBetween = ChronoUnit.WEEKS.between(
                storedWeekStart.atZone(ZoneOffset.UTC).toLocalDate(),
                Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
        );

        if (weeksBetween >= 1) {
            Logger.debug("[WeeklyStatsManager] Rolling over stats. Old week start: " + storedWeekStart);
            Logger.debug("[WeeklyStatsManager] New week start set to: " + computedWeekStart);
            // snapshot old week
            final JSONObject oldStats = getStatsJSON(sendUsageData);

            // rollover
            StatisticsStore.getInstance().resetAll();
            Preferences.getInstance().set(PreferencesType.WEEK_START, computedWeekStart.toString());

            return new WeeklyStats(storedWeekStart, oldStats, getPreferencesJSON(sendUsageData));
        }
        return null;
    }

    private static Instant computeWeekStart() {
        final ZoneId zone = ZoneOffset.UTC;
        final LocalDate today = LocalDate.now(zone);
        final LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        return weekStart.atStartOfDay(zone).toInstant();
    }

    private static Instant loadOrInitWeekStart(Instant defaultStart) {
        try {
            return Instant.parse(Preferences.getInstance().get(PreferencesType.WEEK_START, defaultStart.toString()));
        } catch (DateTimeParseException ex) {
            Preferences.getInstance().set(PreferencesType.WEEK_START, defaultStart.toString());
            return defaultStart;
        }
    }

    public static WeeklyStats getCurrentWeekStats(boolean sendUsageData) {
        final Instant weekStart = loadOrInitWeekStart(WeeklyStatsManager.computeWeekStart());
        return new WeeklyStats(weekStart, getStatsJSON(sendUsageData), getPreferencesJSON(sendUsageData));
    }

    private static JSONObject getStatsJSON(boolean sendUsageData) {
        return sendUsageData ? new JSONObject(StatisticsStore.getInstance().getAll()) : new JSONObject();
    }

    private static JSONObject getPreferencesJSON(boolean sendUsageData) {
        final JSONObject json = new JSONObject();
        json.put("version", Constants.VERSION);
        AppPropertiesUtil.getProps().forEach(json::put);
        if (sendUsageData) {
            Preferences.getInstance().getAllWithPrefix().forEach(json::put);
        } else {
            json.put("preferences.send_usage_data", false);
        }
        return json;
    }
}
