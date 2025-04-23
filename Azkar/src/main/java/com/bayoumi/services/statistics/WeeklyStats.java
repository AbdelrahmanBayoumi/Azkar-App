package com.bayoumi.services.statistics;

import kong.unirest.json.JSONObject;

import java.time.Instant;

public class WeeklyStats {
    public final Instant weekStart;
    public final JSONObject statistics;
    public final JSONObject preferences;

    public WeeklyStats(Instant week, JSONObject stats, JSONObject pref) {
        weekStart = week;
        statistics = stats;
        preferences = pref;
    }
}
