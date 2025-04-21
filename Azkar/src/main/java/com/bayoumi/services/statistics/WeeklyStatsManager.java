package com.bayoumi.services.statistics;

import com.bayoumi.storage.statistics.StatisticsStore;
import com.bayoumi.storage.statistics.StatisticsType;
import com.bayoumi.util.Logger;
import kong.unirest.json.JSONObject;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;

public class WeeklyStatsManager {

    public synchronized static void resetIfNeeded() {
        final Instant now = Instant.now();
        final Instant computedWeekStart = computeWeekStart();
        final Instant storedWeekStart = loadOrInitWeekStart(computedWeekStart);

        final long weeksBetween = ChronoUnit.WEEKS.between(
                storedWeekStart.atZone(ZoneId.systemDefault()).toLocalDate(),
                now.atZone(ZoneId.systemDefault()).toLocalDate()
        );
        if (weeksBetween >= 1) {
            Logger.debug("[WeeklyStatsManager] Rolling over stats. Old week start: " + storedWeekStart);
            try {
                final StatisticsStore store = StatisticsStore.getInstance();
                EnumSet.complementOf(EnumSet.of(StatisticsType.WEEK_START)).forEach(store::reset);
                store.set(StatisticsType.WEEK_START, computedWeekStart.toString());
                Logger.debug("[WeeklyStatsManager] New week start set to: " + computedWeekStart);
            } catch (Exception ex) {
                Logger.error(null, ex, WeeklyStatsManager.class.getName());
            }
        }
    }

    private static Instant computeWeekStart() {
        final ZoneId zone = ZoneId.systemDefault();
        final LocalDate today = LocalDate.now(zone);
        final LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        return weekStart.atStartOfDay(zone).toInstant();
    }

    private static Instant loadOrInitWeekStart(Instant defaultStart) {
        try {
            return Instant.parse(StatisticsStore.getInstance().get(StatisticsType.WEEK_START, defaultStart.toString()));
        } catch (DateTimeParseException ex) {
            StatisticsStore.getInstance().set(StatisticsType.WEEK_START, defaultStart.toString());
            return defaultStart;
        }
    }

    public static JSONObject getStatsJson() {
        return new JSONObject(StatisticsStore.getInstance().getAll());
    }
}
